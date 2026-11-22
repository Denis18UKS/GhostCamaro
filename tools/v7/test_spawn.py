"""Summon one Camaro near the connected player on the disposable QA server."""
from pathlib import Path
import socket
import struct
import sys
config = dict(line.split('=', 1) for line in Path(sys.argv[1]).read_text().splitlines() if '=' in line)
with socket.create_connection(('127.0.0.1', 25579), timeout=10) as sock:
    stream = sock.makefile('rb')
    def request(kind, payload):
        body = struct.pack('<ii', 1, kind) + payload.encode() + bytes(2)
        sock.sendall(struct.pack('<i', len(body)) + body)
        size, = struct.unpack('<i', stream.read(4))
        reply = stream.read(size)
        assert struct.unpack_from('<i', reply)[0] == 1
        return reply[8:-2].decode()
    request(3, config['rcon.password'])
    print(request(2, 'execute at @p run summon ghostcamaro:ghost_camaro ~ ~1 ~2'))
