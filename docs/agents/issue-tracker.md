# Issue tracker: GitHub

このリポジトリの issue と spec は GitHub Issues で管理する。操作はすべて `gh` CLI で行う。

## 規約

- **issue の作成**: `gh issue create --title "..." --body "..."`。複数行の本文は heredoc を使う。
- **issue の閲覧**: `gh issue view <number> --json number,title,body,labels,comments`。
- **issue の一覧**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` に、必要に応じて `--label` / `--state` フィルタを付ける。
- **親 issue の sub-issue にする**: `gh issue create --parent <parent> ...`、または作成後に `gh issue edit <parent> --add-sub-issue <child>`（`gh` 2.94 以上）。古い `gh` では `gh api --method POST repos/<owner>/<repo>/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>`（database id。下記 **Blocking** を参照）。sub-issue が使えない場合は、子 issue 本文の先頭に `Part of #<parent>` と書く。
- **コメント**: `gh issue comment <number> --body "..."`
- **ラベルの付与 / 削除**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **クローズ**: `gh issue close <number> --comment "..."`

リポジトリは `git remote -v` から推定する。clone 内で実行すれば `gh` が自動で判定する。

## triage 対象としての Pull Request

**PR をリクエストの窓口として扱う: no**（このリポジトリで外部 PR を機能リクエストとして扱う場合は `yes` にする。`/triage` がこのフラグを読む）

`yes` の場合、PR も issue と同じラベル・状態で扱い、`gh pr` の対応コマンドを使う。

- **PR の閲覧**: `gh pr view <number> --comments`、差分は `gh pr diff <number>`。
- **triage 対象の外部 PR の一覧**: `gh api --paginate 'repos/{owner}/{repo}/pulls?state=open' --jq '.[] | select(.author_association | IN("OWNER","MEMBER","COLLABORATOR") | not) | {number, title, author: .user.login, author_association, labels: [.labels[].name]}'`。
- **コメント / ラベル / クローズ**: `gh pr comment`、`gh pr edit --add-label`/`--remove-label`、`gh pr close`。

GitHub では issue と PR が同じ番号空間を共有するため、`#42` だけではどちらか分からない。`gh pr view 42` で確認し、失敗したら `gh issue view 42` を試す。

## スキルが「issue tracker に公開する」と言ったとき

GitHub issue を作成する。

## スキルが「関連チケットを取得する」と言ったとき

上記 **issue の閲覧** の手順で読む。

## Wayfinding の操作

`/wayfinder` が使う。**map** は1つの issue で、その **子** issue がチケットになる。

- **Map**: `wayfinder:map` ラベルの付いた1つの issue。本文に Notes / Decisions-so-far / Fog を持つ。`gh issue create --label wayfinder:map`。
- **子チケット**: map に GitHub sub-issue として紐づけた issue（**親 issue の sub-issue にする** を参照）。sub-issue が使えない場合は、map 本文のタスクリストに子を追加し、子の本文先頭に `Part of #<map>` と書く。ラベルは `wayfinder:<type>`（`research`/`prototype`/`grilling`/`task`）。着手したら担当開発者を assign する。
- **Blocking**: GitHub の **ネイティブな issue dependencies** を正とする（UI でも見える）。依存の追加は `gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`。`<blocker-db-id>` はブロック元の数値 **database id**（`gh api repos/<owner>/<repo>/issues/<n> --jq .id`。`#number` や `node_id` ではない）。GitHub は `issue_dependencies_summary.blocked_by`（未完了のブロッカーのみ）を返し、これが現時点のゲートになる。dependencies が使えない場合は、子の本文先頭に `Blocked by: #<n>, #<n>` と書く。すべてのブロッカーがクローズされたらチケットはブロック解除となる。
- **Frontier の取得**: map の open な子を列挙し（`gh issue list --state open` を map の sub-issue / タスクリストに絞る）、未完了のブロッカーがあるもの（`issue_dependencies_summary.blocked_by > 0`、または `Blocked by` 行に open な issue がある）や assignee がいるものを除く。map 内で先に並んでいるものを優先する。
- **Claim**: `gh issue edit <n> --add-assignee @me`。セッションで最初に行う書き込みとする。
- **Resolve**: `gh issue comment <n> --body "<answer>"` → `gh issue close <n>` → map の Decisions-so-far に要点とリンクを追記する。
