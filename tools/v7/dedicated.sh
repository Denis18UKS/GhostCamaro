#!/usr/bin/env bash
set -eu
home_dir=run/network-server
mkdir -p "$home_dir"
printf 'eula=true\n' > "$home_dir/eula.txt"
printf 'online-mode=false\nserver-ip=127.0.0.1\nserver-port=25578\nlevel-type=minecraft:flat\nview-distance=3\nsimulation-distance=3\nspawn-protection=0\n' > "$home_dir/server.properties"
# The local CI operator sends the spawn command through RCON on a disposable loopback server.
secret=$(openssl rand -hex 16)
printf 'enable-rcon=true\nrcon.password=%s\nrcon.port=25579\n' "$secret" >> "$home_dir/server.properties"
bash gradlew -PnetworkSmoke runNetworkServer > "$home_dir/launch.log" 2>&1 &
server_proc=$!
trap 'kill "$server_proc" 2>/dev/null || true' EXIT
for i in {1..180}; do
    grep -q 'Done (' "$home_dir/launch.log" && break
    kill -0 "$server_proc" || { tail -40 "$home_dir/launch.log"; exit 1; }
    sleep 2
done
grep -q 'Done (' "$home_dir/launch.log"
(
    for i in {1..180}; do
        grep -q 'joined the game' "$home_dir/launch.log" && break
        sleep 2
    done
    python3 tools/v7/test_spawn.py "$home_dir/server.properties"
) &
GHOST_DEDICATED_CLIENT=1 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null xvfb-run -a bash gradlew -PvehicleSmoke runClient
test -s run/evidence/dedicated-client.txt
