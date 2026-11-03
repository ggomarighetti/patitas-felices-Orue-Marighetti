import json
import math
import subprocess
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path
from threading import Barrier
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, ProxyHandler, build_opener

root = Path(__file__).resolve().parents[3]
directory = Path(__file__).resolve().parent
opener = build_opener(ProxyHandler({}))
base = 'http://127.0.0.1:8080'
admin = '/ms-turno/actuator/'
started = datetime.now(timezone.utc).isoformat()
created = {'turnos': [], 'veterinarios': [], 'mascotas': [], 'duenos': []}
stopped = set()


def compose(*args):
    result = subprocess.run(['docker', 'compose', '--profile', 'app', *args], cwd=root,
                            capture_output=True, text=True, encoding='utf-8', timeout=180)
    assert result.returncode == 0, result.stderr[-1800:]
    return result.stdout


def request(method, path, body=None):
    headers = {'Accept': 'application/json'}
    payload = None
    if body is not None:
        headers['Content-Type'] = 'application/json'
        payload = json.dumps(body).encode('utf-8')
    before = time.monotonic()
    try:
        response = opener.open(Request(base + path, data=payload, headers=headers, method=method), timeout=60)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read().decode('utf-8')
        data = json.loads(raw) if raw else None
        return dict(status=response.code, durationMs=round((time.monotonic() - before) * 1000, 2),
                    headers={key.lower(): value for key, value in response.headers.items()}, body=data)


def get(path):
    response = request('GET', path)
    assert response['status'] == 200, response
    return response['body']


def create(resource, body):
    response = request('POST', '/api/' + resource, body)
    assert response['status'] == 201, response
    created[resource].append(response['body']['id'])
    return response


def circuit_health():
    response = request('GET', admin + 'health')
    assert response['status'] in [200, 503], response
    return response


def state(health, name):
    return health['body']['components']['circuitBreakers']['details'][name]['details']['state']


def retry_events(name):
    return [event for event in get(admin + 'retryevents')['retryEvents'] if event['retryName'] == name]


def metric_count():
    query = urlencode([('tag', 'uri:/api/turnos'), ('tag', 'method:GET'), ('tag', 'status:200')])
    data = get(admin + 'metrics/http.server.requests?' + query)
    return next(item['value'] for item in data['measurements'] if item['statistic'] == 'COUNT')


def wait_until_readable(resource):
    for attempt in range(60):
        response = request('GET', '/api/' + resource)
        if response['status'] == 200:
            return
        assert response['status'] in [500, 502, 503], response
        time.sleep(2)
    raise AssertionError('El recurso no volvió a estar disponible: ' + resource)


def wait_for_turno_discovery(service):
    before = time.monotonic()
    for attempt in range(90):
        health = circuit_health()
        applications = health['body']['components']['discoveryComposite']['components']['eureka']['details']['applications']
        if applications.get(service.upper()) == 1:
            break
        time.sleep(1)
    else:
        raise AssertionError('Turnos todavía no descubrió la dependencia recuperada: ' + service)
    discovery_wait = round((time.monotonic() - before) * 1000, 2)
    time.sleep(36)
    return dict(registeredInTurnoClient=True, registryWaitMs=discovery_wait, loadBalancerCacheWaitSeconds=36)


def failure_and_recovery(service, circuit_name, message, turno):
    baseline = get('/api/turnos')
    health_before = circuit_health()
    assert state(health_before, circuit_name) == 'CLOSED'
    stopped.add(service)
    compose('stop', service)
    failures = []
    for attempt in range(9):
        response = request('POST', '/api/turnos', turno)
        assert response['status'] == 503 and message in response['body']['mensaje'], response
        assert get('/api/turnos') == baseline
        failures.append(dict(status=response['status'], durationMs=response['durationMs'], body=response['body']))
        open_health = circuit_health()
        if state(open_health, circuit_name) == 'OPEN':
            break
    else:
        raise AssertionError('El circuito no llegó a OPEN')
    open_details = open_health['body']['components']['circuitBreakers']['details'][circuit_name]['details']
    assert open_details['bufferedCalls'] >= 5 and open_details['failedCalls'] == len(failures)
    assert float(open_details['failureRate'].removesuffix('%')) >= 50
    assert open_health['body']['components']['circuitBreakers']['details'][circuit_name]['status'] == 'CIRCUIT_OPEN'
    readiness = get(admin + 'health/readiness')
    assert readiness['status'] == 'UP'
    before_rejected = retry_events(circuit_name)
    fast_responses = []
    for attempt in range(3):
        response = request('POST', '/api/turnos', turno)
        assert response['status'] == 503 and response['durationMs'] < 1000, response
        fast_responses.append(dict(status=response['status'], durationMs=response['durationMs']))
    assert retry_events(circuit_name) == before_rejected
    assert get('/api/turnos') == baseline
    print(service + ': OPEN, fallback 503 inmediato y sin escrituras', flush=True)
    for attempt in range(40):
        half_health = circuit_health()
        if state(half_health, circuit_name) == 'HALF_OPEN':
            break
        assert state(half_health, circuit_name) == 'OPEN'
        time.sleep(0.4)
    else:
        raise AssertionError('No se produjo la transición automática a HALF_OPEN')
    compose('start', '--wait', service)
    stopped.remove(service)
    warmup = wait_for_turno_discovery(service)
    print(service + ': registro de Turnos y caché actualizados; comprobando recuperación', flush=True)
    probes = []
    for probe in range(3):
        response = create('turnos', turno)
        assert response['body']['estado'] == 'PENDIENTE'
        current_health = circuit_health()
        expected_state = 'CLOSED' if probe == 2 else 'HALF_OPEN'
        assert state(current_health, circuit_name) == expected_state, current_health
        probes.append(dict(status=201, state=expected_state, body=response['body']))
    transitions = [event for event in get(admin + 'circuitbreakerevents')['circuitBreakerEvents']
                   if event['circuitBreakerName'] == circuit_name and event['type'] == 'STATE_TRANSITION']
    observed = [event['stateTransition'] for event in transitions]
    assert 'State transition from CLOSED to OPEN' in observed, observed
    assert 'State transition from OPEN to HALF_OPEN' in observed, observed
    assert 'State transition from HALF_OPEN to CLOSED' in observed, observed
    events = retry_events(circuit_name)
    assert any(event['type'] == 'RETRY' and event['numberOfAttempts'] == 1 for event in events)
    assert any(event['type'] == 'RETRY' and event['numberOfAttempts'] == 2 for event in events)
    print(service + ': tres pruebas correctas, HALF_OPEN -> CLOSED', flush=True)
    result = dict(service=service, circuit=circuit_name, closedHealth=health_before,
                  failingRequests=failures, openHealth=open_health, readinessDuringOpen=readiness,
                  rejectedWithoutRetries=fast_responses, halfOpenHealth=half_health,
                  discoveryRecovery=warmup, probes=probes, transitions=transitions, retryEvents=events)
    (root / 'tmp' / ('sprint-13-' + service + '-observacion.json')).write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return result


scenarios = []
try:
    for resource in created:
        assert get('/api/' + resource) == []
    initial_health = circuit_health()
    assert all(state(initial_health, name) == 'CLOSED' for name in ['mascotaService', 'veterinarioService'])
    owner = create('duenos', dict(nombre='Ana', apellido='Prueba', email='sprint13@example.com', telefono='1112345678'))['body']
    pet = create('mascotas', dict(nombre='Luna', especie='Gato', raza='Siamés', edad=3, duenoId=owner['id']))['body']
    vet = create('veterinarios', dict(nombre='Marta', apellido='Prueba', especialidad='Felinos', email='sprint13-vet@example.com'))['body']
    turno = dict(fechaHora='2026-12-20T10:00:00', motivo='Verificación de resiliencia', mascotaId=pet['id'], veterinarioId=vet['id'])
    normal = create('turnos', turno)
    assert normal['body']['estado'] == 'PENDIENTE'
    normal_health = circuit_health()
    assert all(state(normal_health, name) == 'CLOSED' for name in ['mascotaService', 'veterinarioService'])
    for service, name, message in [('ms-mascota', 'mascotaService', 'servicio de mascotas'),
                                    ('ms-veterinario', 'veterinarioService', 'servicio de veterinarios')]:
        scenarios.append(failure_and_recovery(service, name, message, turno))
    final_health = circuit_health()
    assert final_health['body']['status'] == 'UP'
    metrics = get(admin + 'metrics/resilience4j.circuitbreaker.state')
    evidence = dict(executedAt=started, normalCreation=normal, normalHealth=normal_health,
                    scenarios=scenarios, finalHealth=final_health, circuitStateMetric=metrics)
finally:
    for service in list(stopped):
        compose('start', '--wait', service)
        stopped.remove(service)
    for resource, identifiers in created.items():
        if identifiers:
            wait_until_readable(resource)
        for identifier in reversed(identifiers):
            response = request('DELETE', '/api/' + resource + '/' + str(identifier))
            assert response['status'] == 204, response

evidence['cleanup'] = {resource: get('/api/' + resource) for resource in created}
assert all(body == [] for body in evidence['cleanup'].values())
(directory / 'sprint-13-resiliencia.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

time.sleep(2.2)
assert get('/api/turnos') == []
before_count = metric_count()
time.sleep(2.2)
barrier = Barrier(60)


def burst(index):
    path = '/api/turnos' if index % 2 == 0 else '/ms-turno/api/turnos'
    barrier.wait(timeout=20)
    response = request('GET', path)
    return dict(path=path, status=response['status'], durationMs=response['durationMs'],
                headers={key: value for key, value in response['headers'].items() if key.startswith('x-ratelimit')})


burst_start = time.monotonic()
with ThreadPoolExecutor(max_workers=60) as executor:
    results = list(executor.map(burst, range(60)))
duration = time.monotonic() - burst_start
assert all(result['status'] in [200, 429] for result in results)
accepted = sum(result['status'] == 200 for result in results)
rejected = sum(result['status'] == 429 for result in results)
assert accepted > 0 and rejected > 0
assert accepted <= 20 + 10 * (math.ceil(duration) + 1)
for result in results:
    assert result['headers']['x-ratelimit-replenish-rate'] == '10'
    assert result['headers']['x-ratelimit-burst-capacity'] == '20'
time.sleep(2.2)
after_count = metric_count()
assert after_count - before_count == accepted, (before_count, after_count, accepted)
assert get('/api/turnos') == []
redis_keys = compose('exec', '-T', 'redis', 'redis-cli', '--scan', '--pattern', 'request_rate_limiter.*').splitlines()
assert len(redis_keys) == 2 and all('ms-turno' in key for key in redis_keys), redis_keys
rate = dict(executedAt=datetime.now(timezone.utc).isoformat(),requests=60,accepted=accepted,rejected=rejected,
            durationMs=round(duration * 1000, 2),results=results,redisKeys=sorted(redis_keys),
            backendGetCountBefore=before_count,backendGetCountAfter=after_count,
            rejectedRequestsDidNotReachBackend=True,recoveredAfterTokenRefill=True,
            sharedQuotaBetweenCanonicalAndAlias=True)
(directory / 'sprint-13-rate-limiter.json').write_text(json.dumps(rate, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Ráfaga: ' + str(accepted) + ' aceptadas, ' + str(rejected) + ' HTTP 429; rechazos sin llegar a Turnos', flush=True)
