#!/usr/bin/env sh
# Usage: ./run.sh soap-server | rmi-server | portal | soap-client | rmi-client | soap-simple | rmi-simple   [extra args]
set -e
ROOT=$(cd "$(dirname "$0")" && pwd)
cd "$ROOT"
[ -f dscc-portal/target/dscc-portal.jar ] || ./build-all.sh

EMP="$ROOT/employee-soap/target/employee-soap.jar:$ROOT/employee-soap/target/lib/*"
LIBR="$ROOT/library-rmi/target/library-rmi.jar:$ROOT/library-rmi/target/lib/*"
PORTAL="$ROOT/dscc-portal/target/dscc-portal.jar:$ROOT/dscc-portal/target/lib/*"

TARGET=${1:-}
[ $# -gt 0 ] && shift

# servers run inside their module folder, so their SQLite files live in <module>/data
case "$TARGET" in
  soap-server) cd employee-soap && exec java -cp "$EMP" employee.server.EmployeePublisher "$@" ;;
  rmi-server)  cd library-rmi && exec java -cp "$LIBR" library.server.LibraryServer "$@" ;;
  portal)      exec java -cp "$PORTAL" dscc.portal.PortalApp ;;
  soap-client) exec java -cp "$EMP" employee.client.ui.EmployeeClientApp ;;
  rmi-client)  exec java -cp "$LIBR" library.client.ui.LibraryClientApp ;;
  soap-simple) exec java -cp "$EMP" employee.client.EmployeeClientGUI ;;
  rmi-simple)  exec java -cp "$LIBR" library.client.LibraryClientGUI ;;
  *)
    echo "Usage: ./run.sh soap-server | rmi-server | portal | soap-client | rmi-client | soap-simple | rmi-simple"
    exit 1 ;;
esac
