#!/bin/bash
# ===========================================
# Block all foreign IPs - only allow IPs listed in vn.txt
# Applies to all ports, including ICMP (ping)
# ===========================================

# File containing allowed IP ranges (Vietnam IPs)
VN_FILE="vn.txt"

# Name of the ipset
SET_NAME="vn_allow"

echo "[*] Creating ipset for allowed IPs..."

# Delete existing ipset if exists
ipset destroy $SET_NAME 2>/dev/null

# Create new ipset of type hash:net, with a maximum of 200000 entries
ipset create $SET_NAME hash:net maxelem 200000

# Read each line (IP/subnet) from the file and add to ipset
while read -r NET; do
    [[ -z "$NET" ]] && continue
    ipset add $SET_NAME $NET
done < "$VN_FILE"

echo "[*] Flushing existing iptables INPUT chain..."
# Flush existing rules in INPUT chain
iptables -F INPUT

echo "[*] Configuring iptables rules..."

# Allow localhost
iptables -A INPUT -i lo -j ACCEPT

# Allow established and related connections
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# Allow all traffic from VN IPs in ipset
iptables -A INPUT -m set --match-set $SET_NAME src -j ACCEPT

# Drop everything else, including ICMP (ping)
iptables -A INPUT -j DROP

echo "[+] Done. Only IPs listed in $VN_FILE can access the server, all foreign IPs are blocked."
