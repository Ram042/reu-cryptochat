#!/bin/bash
set -e

rm -rf cli/build/demo

./server.sh >&2 &
server=$!
echo "Server PID ${server}"

set -x
./cli.sh profile --dbPath=demo/db-alice init 1>&2
./cli.sh profile --dbPath=demo/db-bob init 1>&2

./cli.sh profile --dbPath=demo/db-alice print 1>&2
./cli.sh profile --dbPath=demo/db-bob print 1>&2
{ set +x; } 2>/dev/null

alice=$(./cli.sh profile --dbPath=demo/db-alice print | cut -d " " -f 3 | head -n 1)
bob=$(./cli.sh profile --dbPath=demo/db-bob print | cut -d " " -f 3 | head -n 1)

echo "Alice: ${alice}"
echo "Bob: ${bob}"

set -x
./cli.sh profile --dbPath=demo/db-alice publish 1>&2
./cli.sh profile --dbPath=demo/db-bob publish 1>&2

./cli.sh session --dbPath=demo/db-alice init $bob 1>&2
./cli.sh session --dbPath=demo/db-bob get 1>&2
./cli.sh session --dbPath=demo/db-bob reply $alice 1>&2
./cli.sh session --dbPath=demo/db-alice get 1>&2

./cli.sh message --dbPath=demo/db-alice send --target $bob 'Hello, World!'
./cli.sh message --dbPath=demo/db-bob get
./cli.sh message --dbPath=demo/db-bob show --target $alice

{ set +x; } 2>/dev/null

kill $server
