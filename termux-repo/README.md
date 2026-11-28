# termux-studio APT Repository (v1.0.0)

Host the `termux-repo/` directory on GitHub Pages (or publish its contents at a public HTTPS URL) to install `termux-studio` with `pkg`:

```bash
echo "deb [trusted=yes] https://<YOUR_USER>.github.io/termux-studio/termux-repo stable main" > $PREFIX/etc/apt/sources.list.d/termux-studio.list
pkg update -y
pkg install -y termux-studio
termux-studio link-storage
termux-studio probe
```

Or install directly on your phone from your cloned repo / shared storage:

```bash
pkg install -y ./pool/main/t/termux-studio/termux-studio_1.0.0_all.deb
termux-studio link-storage && termux-studio probe
```
