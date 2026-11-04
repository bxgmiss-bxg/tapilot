#!/usr/bin/env bash
set -euo pipefail

script_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
assistant_root="$(cd "${script_root}/.." && pwd -P)"
source "${script_root}/termux-package-cache.sh"

output_directory="${assistant_root}/.cache/native/arm64-v8a"
architecture="${1:-arm64-v8a}"
case "$architecture" in
  arm64-v8a) ;;
  x86_64) output_directory="${assistant_root}/.cache/ci-native/x86_64" ;;
  *) echo "Unsupported runtime architecture: $architecture" >&2; exit 1 ;;
esac
work_directory="${assistant_root}/.cache/node-runtime-work-${architecture}"
staging="${work_directory}/staging"
trap 'rm -rf "${work_directory}"' EXIT

packages=(
  'n/nodejs-lts/nodejs-lts_24.22.2_aarch64.deb 150d7dd382e92587808d96813b9538fe5c6c41fa33f1a1033daa4e30c6ff2695'
  'c/c-ares/c-ares_1.34.9_aarch64.deb 623d84855e176f3f2a415d24c0e1d2f529356f4b5439bb3313e6d7ff0da4585c'
  'libc/libc++/libc++_30_aarch64.deb 53d0b84a7ba7459024257cb94d5b136fe13ef858567f65a8064b35950799f2ca'
  'libi/libicu/libicu_78.3_aarch64.deb f536403f65a08fe0df6e7304184e902d54def77d5c3bd5edfd9109d57601d276'
  'libs/libsqlite/libsqlite_3.54.1-1_aarch64.deb 661fb995f7215845007b222a85266ae99d9443e87e0604f8677edfd738e6b81a'
  'o/openssl/openssl_1%3A3.6.5_aarch64.deb a2fb7856875e63ef6b90442752e05974813372c3ca86d2513a78ea43c67a0f8b'
  'z/zlib/zlib_1.3.2_aarch64.deb 75e7d0af17fcc3b40004309fdc00a1ddb9ae08346dce5e269902c34ac3966ac9'
)


# Verification-only emulator runtime, pinned from the same Termux package index.
if [[ "$architecture" == x86_64 ]]; then
  packages=(
    'n/nodejs-lts/nodejs-lts_24.22.2_x86_64.deb 0dbbc99d03af8093ce2b8029930fcf80b201372ed2cf2275722decdb8f974fbe'
    'c/c-ares/c-ares_1.34.9_x86_64.deb ac96c37fcc9181d1a7095f60485457fe6c3d784a83b0e8e6e7af9b3bfceda237'
    'libc/libc++/libc++_30_x86_64.deb c9bcd9a8067a19c9c0e4893a72d314a2e37d6b67ce8dd05c67b9bbacad044b4f'
    'libi/libicu/libicu_78.3_x86_64.deb 19fa8c4d828719f465d523983b1e0d833e4130bb22790104638965d97e27fe60'
    'libs/libsqlite/libsqlite_3.54.1-1_x86_64.deb 497471806378934fec8a6f3b5db70cf52a3652828786a241e2e37eebc19112b0'
    'o/openssl/openssl_1%3A3.6.5_x86_64.deb 540f144592d23d35672976e175ede1b5d8cf464f153f646df5d4e6dcd8491a15'
    'z/zlib/zlib_1.3.2_x86_64.deb c61b089dd30981452f5953b2bad4c4e7857062b2abd971f87fb0aec441bc02f4'
  )
fi

rm -rf "${staging}"
mkdir -p "${staging}/lib"
unpack_root="${work_directory}/unpack"
for spec in "${packages[@]}"; do
  read -r entry sha256 <<< "${spec}"
  package="$(fetch_termux_package "${entry}" "${sha256}")"
  extract_termux_package "${package}" "${unpack_root}/$(basename "${entry}")"
done

prefix='data/data/com.termux/files/usr'
node_binary="$(find "${unpack_root}" -path "*/${prefix}/bin/node" -type f -print -quit)"
if [[ -z "${node_binary}" ]]; then
  echo 'nodejs-lts package did not contain bin/node' >&2
  exit 1
fi
cp "${node_binary}" "${staging}/node"
for library_name in libz.so.1.3.2 libcares.so libsqlite3.so.3.54.1 \
  libcrypto.so.3 libssl.so.3 libicui18n.so.78.3 libicuuc.so.78.3 \
  libicudata.so.78.3 libc++_shared.so; do
  library="$(find "${unpack_root}" -path "*/${prefix}/lib/${library_name}" -type f -print -quit)"
  if [[ -z "${library}" ]]; then
    echo "Termux packages did not contain ${library_name}" >&2
    exit 1
  fi
  cp "${library}" "${staging}/lib/${library_name}"
done

python3 "${script_root}/patch-node-elf.py" "${staging}" "${output_directory}"
notice_directory="${assistant_root}/app/src/main/assets/third-party/native"
if [[ "$architecture" == x86_64 ]]; then
  notice_directory="${assistant_root}/.cache/ci-native-assets/third-party/native-x86_64"
fi
node "${script_root}/third-party-notices.mjs" native "${unpack_root}" \
  "${notice_directory}"
echo "Node runtime ready at ${output_directory}"
