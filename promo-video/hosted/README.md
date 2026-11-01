# 托管宣传片

2026-11-01 上传，原始片保留在 `../en/` 和 `../zh/`。本目录是实际上传版本，前 73.5 秒演示保持，最后约 4 秒替换为自然语言操作手机的文字片尾，音频在 73–73.5 秒淡出。没有变速。

- YouTube（英文）：https://www.youtube.com/watch?v=TwuMuZ59yog
- 哔哩哔哩（中文）：https://www.bilibili.com/video/BV1ANxb6XEMy/
- 官网英文默认 YouTube，中文默认 B 站；提供来源切换和本站 MP4 备用。
- `endcard-*.svg` 是文字排版源；PNG 用作最终视频叠加层。
- H.264/AAC、1080×1920、faststart。构建需遵循本机 dev-gate heavy 限流。

在仓库根目录重新生成（将 `en` 换为 `zh` 可生成中文版）：

```sh
ffmpeg -i promo-video/en/tapilot-promo.mp4 -i promo-video/hosted/endcard-en.png \
  -filter_complex "[0:v][1:v]overlay=0:0:enable='gte(t,73.5)'[v]" \
  -map '[v]' -map 0:a -af 'afade=t=out:st=73:d=0.5' \
  -c:v libx264 -threads 2 -filter_complex_threads 1 -preset fast -crf 21 \
  -pix_fmt yuv420p -c:a aac -b:a 128k -movflags +faststart \
  promo-video/hosted/tapilot-en.mp4
```

上传账号：YouTube LifeOS for Obsidian；哔哩哔哩 LifeOS-林大师。
发布视频不改变应用仓库可见性，也不代表发布了新 APK。
