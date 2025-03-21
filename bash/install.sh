#!/usr/bin/env bash
# https://dev.to/lissy93/cli-tools-you-cant-live-without-57f6?s=09

sudo apt-get install \
 ripgrep `# better grep` \
 eza `# better ls https://github.com/ogham/exa` \
 duf `# better df https://github.com/muesli/duf` \
 bat `# better cat https://github.com/sharkdp/bat` \
 jq `# https://github.com/stedolan/jq` \
 fzf `# https://github.com/junegunn/fzf` \
 tre-command `# tre, better tree https://github.com/dduan/tre` \
 figlet `# Output text as big ASCII art text http://www.figlet.org/` \
 lolcat `# rainbow https://github.com/busyloop/lolcat` \
 pgcli

# Install Atuin
curl --proto '=https' --tlsv1.2 -LsSf https://setup.atuin.sh | sh
# Install blesh
curl -L https://github.com/akinomyoga/ble.sh/releases/download/nightly/ble-nightly.tar.xz | tar xJf -
bash ble-nightly/ble.sh --install ~/.local/share
echo 'source ~/.local/share/blesh/ble.sh' >> ~/.bashrc
#
echo 'eval "$(atuin init bash)"' >> ~/.bashrc
# End Atuin

sudo snap install \
 procs `# better ps https://github.com/dalance/procs`

# https://atuin.sh/