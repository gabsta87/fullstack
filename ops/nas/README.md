# Read-only log access for Claude on the staging NAS

A dedicated SSH key that can **only** run `venus-logs.sh`: container status and the last lines of the
Venus containers' logs. No shell, no port forwarding, no other command. Revoke it by deleting its line
in `authorized_keys`.

## 1. On the Mac: create the key (no passphrase, it is confined on the NAS side)

    ssh-keygen -t ed25519 -f ~/.ssh/venus_nas_logs -N "" -C "claude-venus-logs"

## 2. On the NAS (SSH with your admin account)

Install a root-owned copy of the script, **outside** the git checkout (the deployment pulls the repo,
so a copy inside it could be changed by any commit):

    sudo install -o root -g root -m 755 /volume1/docker/venus/ops/nas/venus-logs.sh /usr/local/bin/venus-logs

Allow your account to run only these two Docker commands as root without a password:

    sudo grep -n includedir /etc/sudoers        # must show: #includedir /etc/sudoers.d
    echo "$USER ALL=(root) NOPASSWD: /usr/local/bin/docker logs --timestamps --tail * venus-*, /usr/local/bin/docker ps --all --filter name=venus- --format *" | sudo tee /etc/sudoers.d/venus-logs
    sudo chmod 440 /etc/sudoers.d/venus-logs
    sudo visudo -c                              # must say: parsed OK

Add the public key with the forced command (one single line in `~/.ssh/authorized_keys`):

    mkdir -p ~/.ssh && chmod 700 ~/.ssh
    echo 'command="/usr/local/bin/venus-logs",no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty PUBLIC_KEY_HERE' >> ~/.ssh/authorized_keys
    chmod 600 ~/.ssh/authorized_keys
    chmod 755 ~                                 # DSM may leave the home too open: sshd then ignores the keys

`PUBLIC_KEY_HERE` is the whole content of `~/.ssh/venus_nas_logs.pub` from the Mac.

DSM prerequisites: Control Panel > Terminal & SNMP > Enable SSH service; Control Panel > User & Group >
Advanced > Enable user home service. A DSM major update may reset `/etc/sudoers.d`: re-run the sudoers step if `ps` stops working.

## 3. On the Mac: SSH alias

Add to `~/.ssh/config`:

    Host venus-nas-logs
        HostName nas
        User YOUR_NAS_USER
        IdentityFile ~/.ssh/venus_nas_logs
        IdentitiesOnly yes

## 4. Check

    ssh venus-nas-logs ps                          # container status
    ssh venus-nas-logs logs venus-backend 50       # last 50 backend lines
    ssh venus-nas-logs whoami                      # must be refused (usage message)

## Revoke

Delete the `claude-venus-logs` line from `~/.ssh/authorized_keys` on the NAS and `/etc/sudoers.d/venus-logs`.
