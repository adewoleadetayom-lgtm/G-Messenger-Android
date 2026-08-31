# G Messenger v16 — safe GitHub build

This package is intended to be added on a separate branch named `v16-final` so the existing `main`/older versions are left untouched.

## Safe flow

```bash
git clone https://github.com/adewoleadetayom-lgtm/G-Messenger-Android.git
cd G-Messenger-Android
git switch -c v16-final
```

Copy the contents of this project into the checkout without deleting the existing history, then:

```bash
git status
git add .
git commit -m "G Messenger v16 final: preserve approved UI and add GitHub build"
git push -u origin v16-final
```

GitHub Actions will build the debug APK from this branch. It does not force-push or modify `main`.
