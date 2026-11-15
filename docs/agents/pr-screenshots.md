# PR 本文にスクリーンショットを貼る

`gh pr create` / `gh pr edit` では画像を添付できないので、`pr-assets` ブランチに上げて raw URL で埋め込む。

```sh
scripts/shot.sh after-week                      # build/shots/after-week.full.png を原寸で使う
scripts/upload-pr-assets.py <PR番号> build/shots/after-week.full.png ...
```

出力された `<img ...>` 行を PR 本文に貼り、`gh pr edit <PR番号> --body-file <file>` で更新する。PR 番号は `gh pr create` の後に決まるので、作成 → アップロード → 本文更新の順になる。

`pr-assets` はアプリの履歴と無関係な孤立ブランチで、過去の PR 本文の画像もここを参照している。削除・force push しない。
