#!/system/bin/sh

echo "=== Root Script Executor Test ==="
echo "Script executed from: $(pwd)"
echo "Current user: $(whoami)"
echo "Process ID: $$"
echo "Parent Process ID: $PPID"
echo ""

echo "=== System Information ==="
echo "Android Version: $(getprop ro.build.version.release)"
echo "Device Model: $(getprop ro.product.model)"
echo "Build ID: $(getprop ro.build.id)"
echo ""

echo "=== Mount Namespace Test ==="
echo "Current mount namespace:"
ls -la /proc/self/ns/mnt
echo ""

echo "=== Process Tree ==="
ps -ef | grep -E "(system_server_watchdog|$$|$PPID)" | head -10
echo ""

echo "=== File System Test ==="
echo "Creating test file in /data/adb/5/test_output.txt"
echo "Test executed at: $(date)" > /data/adb/5/test_output.txt
ls -la /data/adb/5/test_output.txt
echo ""

echo "=== Network Test ==="
echo "Checking network connectivity..."
ping -c 1 8.8.8.8 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "Network: Connected"
else
    echo "Network: Disconnected"
fi
echo ""

echo "=== Script Completed Successfully ==="
echo "All operations completed in isolated mount namespace"