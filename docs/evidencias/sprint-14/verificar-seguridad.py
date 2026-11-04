import base64
import hashlib
import hmac
import json
import re
import subprocess
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path
from threading import Barrier
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, ProxyHandler, build_opener

root = Path(__file__).resolve().parents[3]
directory = Path(__file__).resolve().parent
temporary = root / 'tmp'
temporary.mkdir(exist_ok=True)
settings = dict(line.split('=', 1) for line in (root / '.env').read_text(encoding='utf-8-sig').splitlines() if '=' in line and not line.startswith('#'))
secret = settings['JWT_SECRET'].encode('utf-8')
assert len(secret) >= 32
opener = build_opener(ProxyHandler({}))
base = 'http://127.0.0.1:8080'
prefix = 's14_' + uuid.uuid4().hex[:12]
username = 'jorge_' + uuid.uuid4().hex[:12]
owned = [username, prefix + '_vet', prefix + '_recep', prefix + '_race']
started = datetime.now(timezone.utc).isoformat()
jwt_pattern = re.compile(r'eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+')


def encode(value):
    return base64.urlsafe_b64encode(value).rstrip(b'=').decode('ascii')


def signed_token(claims, key=secret, algorithm='HS256'):
    header = encode(json.dumps(dict(alg=algorithm, typ='JWT'), separators=(',', ':')).encode())
    payload = encode(json.dumps(claims, separators=(',', ':')).encode())
    unsigned = header + '.' + payload
    digest = hashlib.sha512 if algorithm == 'HS512' else hashlib.sha256
    return unsigned + '.' + encode(hmac.new(key, unsigned.encode(), digest).digest())


def token_claims(token):
    part = token.split('.')[1]
    return json.loads(base64.urlsafe_b64decode(part + '=' * (-len(part) % 4)))


def redact(value):
    if isinstance(value, dict):
        return {key: '[REDACTED]' if key.lower() in ['token', 'authorization', 'jwt_secret'] else redact(item) for key, item in value.items()}
    if isinstance(value, list):
        return [redact(item) for item in value]
    if isinstance(value, str):
        return jwt_pattern.sub('[REDACTED]', value).replace(secret.decode(), '[REDACTED]')
    return value


def http(method, path, body=None, token=None):
    headers = {'Accept': 'application/json'}
    payload = None
    if body is not None:
        headers['Content-Type'] = 'application/json'
        payload = json.dumps(body).encode()
    if token:
        headers['Authorization'] = 'Bearer ' + token
    before = time.monotonic()
    try:
        response = opener.open(Request(base + path, data=payload, headers=headers, method=method), timeout=30)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read().decode('utf-8')
        return dict(status=response.code, body=json.loads(raw) if raw else None, durationMs=round((time.monotonic() - before) * 1000, 2),
                    headers={key.lower(): value for key, value in response.headers.items()})


def sql(statement):
    command = ['docker', 'compose', '--profile', 'app', 'exec', '-T', 'mysql-auth', 'sh', '-c',
               'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" --batch --skip-column-names patitas_auth']
    result = subprocess.run(command, cwd=root, input=statement, text=True, encoding='utf-8', capture_output=True, timeout=30)
    assert result.returncode == 0, redact(result.stderr)
    return [line.split('\t') for line in result.stdout.splitlines() if line]


def get(path, token):
    response = http('GET', path, token=token)
    assert response['status'] == 200, redact(response)
    return response['body']


def metric(token, uri):
    query = urlencode([('tag', 'uri:' + uri), ('tag', 'method:GET'), ('tag', 'status:200')])
    data = get('/ms-mascota/actuator/metrics/http.server.requests?' + query, token)
    return next(item['value'] for item in data['measurements'] if item['statistic'] == 'COUNT')


def summarize_newman(data):
    run = data['run']
    assert not run['failures'], redact([dict(name=item['source']['name'], message=item['error']['message']) for item in run['failures']])
    results = []
    token = None
    for execution in run['executions']:
        response = execution['response']
        raw = bytes(response.get('stream', {}).get('data', [])).decode('utf-8')
        body = json.loads(raw) if raw else None
        request = execution['request']
        url = request['url']
        resolved = url['protocol'] + '://' + '.'.join(url['host']) + (':' + url['port'] if url.get('port') else '') + '/' + '/'.join(url['path'])
        if url.get('query'):
            resolved += '?' + '&'.join(item['key'] + '=' + item.get('value', '') for item in url['query'])
        assertions = [dict(name=item['assertion'], passed=not item.get('error'), skipped=item.get('skipped', False)) for item in execution.get('assertions', [])]
        assert all(item['passed'] and not item['skipped'] for item in assertions)
        headers = {item['key']: item['value'] for item in response['header'] if item['key'].lower() in ['location', 'www-authenticate', 'access-control-allow-origin', 'access-control-allow-methods', 'x-ratelimit-replenish-rate', 'x-ratelimit-burst-capacity']}
        result = dict(name=execution['item']['name'], method=request['method'], url=resolved, status=response['code'], responseTimeMs=response['responseTime'], headers=headers, body=body, assertions=assertions)
        if isinstance(body, dict) and 'token' in body:
            result['tokenClaims'] = token_claims(body['token'])
            result['tokenFingerprintSha256'] = hashlib.sha256(body['token'].encode()).hexdigest()
            if 'Login de usuario registrado' in result['name']:
                token = body['token']
        results.append(redact(result))
    assert token
    summary = dict(requests=run['stats']['requests']['total'], assertions=run['stats']['assertions']['total'], failures=0,
                   durationMs=run['timings']['completed'] - run['timings']['started'])
    report = dict(formatVersion=1, tool='Newman 6.2.3', executedAt=datetime.fromtimestamp(run['timings']['started'] / 1000, timezone.utc).isoformat(),
                  collection=data['collection']['info']['name'], summary=summary, results=results)
    (directory / 'sprint-14-jwt.newman-results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return token, summary


for attempt in range(120):
    probe = http('POST', '/auth/login', dict(username='sprint14_ready_probe', password='1234'))
    if probe['status'] == 401 and probe['body'] and probe['body'].get('mensaje') == 'Usuario o contraseña incorrectos':
        break
    time.sleep(1)
else:
    raise AssertionError('La ruta pública de autenticación no llegó a estar disponible')

assert all(re.fullmatch(r'[A-Za-z0-9_]+', value) for value in owned)
sql_names = ','.join("'" + value + "'" for value in owned)
baseline_users = int(sql('SELECT COUNT(*) FROM usuarios;')[0][0])
assert int(sql('SELECT COUNT(*) FROM usuarios WHERE username IN (' + sql_names + ');')[0][0]) == 0
now = int(time.time())
claims = dict(sub=username, rol='ADMIN', iat=now - 2, exp=now + 3600)
control = signed_token(claims)
tampered = control.split('.')
tampered[1] = encode(json.dumps(dict(claims, sub='administrador')).encode())
environment = dict(name='Sprint 14 - ejecución local privada', values=[dict(key=key, value=value, enabled=True, type='secret') for key, value in dict(
    authUsername=username, fixturePrefix=prefix, controlToken=control,
    expiredToken=signed_token(dict(claims, iat=now - 120, exp=now - 60)),
    foreignToken=signed_token(claims, key=b'foreign-test-key-not-trusted-by-the-running-services'),
    futureToken=signed_token(dict(claims, nbf=now + 3600)),
    unsignedToken=encode(b'{"alg":"none"}') + '.' + encode(json.dumps(claims).encode()) + '.',
    noExpirationToken=signed_token({key: value for key, value in claims.items() if key != 'exp'}),
    numericRoleToken=signed_token(dict(claims, rol=123)),
    otherAlgorithmToken=signed_token(claims, algorithm='HS512'), tamperedToken='.'.join(tampered)).items()])
environment_path = temporary / 'sprint-14-private.postman_environment.json'
raw_path = temporary / 'sprint-14-newman-full.json'
environment_path.write_text(json.dumps(environment), encoding='utf-8')
evidence = None
try:
    command = ['npx.cmd', '--yes', 'newman', 'run', str(directory / 'sprint-14-jwt.postman_collection.json'),
               '--environment', str(environment_path), '--delay-request', '120', '--reporters', 'cli,json',
               '--reporter-json-export', str(raw_path), '--disable-unicode']
    result = subprocess.run(command, cwd=root, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=240)
    (temporary / 'sprint-14-newman.log').write_text(redact(result.stdout + result.stderr), encoding='utf-8')
    assert result.returncode == 0, redact((result.stdout + result.stderr)[-10000:])
    token, summary = summarize_newman(json.loads(raw_path.read_text(encoding='utf-8-sig')))
    print(str(summary['requests']) + ' solicitudes y ' + str(summary['assertions']) + ' verificaciones Newman aprobadas', flush=True)
    body = dict(username=owned[-1], password='1234', rol='ADMIN')
    barrier = Barrier(2)

    def register_concurrently(index):
        barrier.wait(timeout=10)
        return http('POST', '/auth/register', body)

    with ThreadPoolExecutor(max_workers=2) as executor:
        race = list(executor.map(register_concurrently, range(2)))
    assert sorted(response['status'] for response in race) == [200, 409], redact(race)
    rows = sql("SELECT id,username,rol,LENGTH(password),LEFT(password,4) IN ('$2a$','$2b$','$2y$'),password='1234' FROM usuarios WHERE username IN (" + sql_names + ') ORDER BY username;')
    assert len(rows) == 4
    assert all(row[3:] == ['60', '1', '0'] for row in rows)
    salted = sql('SELECT COUNT(DISTINCT password) FROM usuarios WHERE username IN (' + sql_names + ');')
    assert int(salted[0][0]) == 4
    unique = sql("SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA='patitas_auth' AND TABLE_NAME='usuarios' AND COLUMN_NAME='username' AND NON_UNIQUE=0;")
    assert int(unique[0][0]) >= 1
    assert get('/api/mascotas', token) == []
    before_count = metric(token, '/api/mascotas')
    with ThreadPoolExecutor(max_workers=16) as executor:
        rejected = list(executor.map(lambda index: http('GET', '/api/mascotas', token=None if index % 2 == 0 else 'falso'), range(40)))
    assert all(response['status'] == 401 for response in rejected)
    after_count = metric(token, '/api/mascotas')
    assert after_count == before_count
    time.sleep(2.2)
    barrier = Barrier(60)

    def authenticated_burst(index):
        path = '/api/turnos' if index % 2 == 0 else '/ms-turno/api/turnos'
        barrier.wait(timeout=20)
        response = http('GET', path, token=token)
        return dict(path=path, status=response['status'], durationMs=response['durationMs'])

    with ThreadPoolExecutor(max_workers=60) as executor:
        burst = list(executor.map(authenticated_burst, range(60)))
    assert all(response['status'] in [200, 429] for response in burst)
    accepted = sum(response['status'] == 200 for response in burst)
    limited = sum(response['status'] == 429 for response in burst)
    assert accepted > 0 and limited > 0
    time.sleep(2.2)
    health = get('/actuator/health', token)
    assert health['status'] == 'UP'
    data = {resource: get('/api/' + resource, token) for resource in ['duenos', 'mascotas', 'veterinarios', 'turnos']}
    assert all(body == [] for body in data.values())
    evidence = dict(executedAt=started, newman=summary,
                    database=dict(name='patitas_auth', rows=[dict(id=int(row[0]), username=row[1], rol=row[2], bcryptLength=int(row[3]), bcryptPrefix=True, plaintextStored=False) for row in rows], uniqueUsernameIndex=True, saltedHashesAreDifferent=True),
                    concurrentRegistration=dict(statuses=sorted(response['status'] for response in race), rowsForUsername=1),
                    unauthorizedBeforeBackend=dict(requests=40, statuses=[response['status'] for response in rejected], backendGetCountBefore=before_count, backendGetCountAfter=after_count),
                    authenticatedRateLimiter=dict(requests=60, accepted=accepted, rejected=limited, results=burst, tokenAcceptedAfterRefill=True),
                    authenticatedHealth=health, businessDataAfterVerification=data)
finally:
    sql('DELETE FROM usuarios WHERE username IN (' + sql_names + ');')
    assert int(sql('SELECT COUNT(*) FROM usuarios WHERE username IN (' + sql_names + ');')[0][0]) == 0
    assert int(sql('SELECT COUNT(*) FROM usuarios;')[0][0]) == baseline_users
    environment_path.unlink(missing_ok=True)
    if raw_path.exists() and evidence is not None:
        raw_path.unlink()

evidence['cleanup'] = dict(ownUsersRemoved=4, totalUsersRestored=baseline_users)
(directory / 'sprint-14-seguridad.json').write_text(json.dumps(redact(evidence), ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('BCrypt, registro concurrente, 40 rechazos previos al backend y ráfaga autenticada comprobados', flush=True)
print('Usuarios propios eliminados; datos de negocio vacíos; tokens redactados en las evidencias', flush=True)
