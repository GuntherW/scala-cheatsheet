```shell
curl https://api.github.com/repos/jqlang/jq > repo.json
curl https://api.github.com/repos/jqlang/jq/issues?per_page=5 > issues.json
curl https://api.github.com/repos/jqlang/jq/issues/3088 > issue.json
```

```shell
cat repo.json | jq '.'
```

```shell
cat repo.json  | jq '.owner.login'
```

```shell
cat issues.json | jq '.[].title'
```

#### Array construction
```shell
cat issues.json | jq '[ .[].title ]'
```

#### Object construction
```shell
cat issues.json | jq '[{title: .[].title, number: .[].number }]'
```
#### Object construction with pipe (same as above)
```shell
cat issues.json | jq '[.[] | {title,number} ]'
```
#### Filtering
```shell
cat repo.json | jq '{name, url}'
```

#### Sorting and counting
```shell
echo '["1", "3", "2"]' | jq 'sort'
echo '["1", "3", "2"]' | jq 'reverse'
echo '["1", "3", "2"]' | jq 'length'
```
```shell
cat issues.json | jq '[.[] | {title}] | sort'
```
```shell
cat issue.json | jq '{title, number, labels: .labels | sort }' # sort by first element (id)
```
```shell
cat issue.json | jq '{title, number, labels: .labels | sort_by(.name) }' 
```
```shell
cat issue.json | jq '{title, number, labels: .labels | length }' 
```

#### Map syntax
```shell
cat issues.json | jq '[.[] | {title}] | sort'
```
is the same as
```shell
cat issues.json | jq 'map({title}) | sort'
```

#### Filtering mit select
```shell
cat issues.json | jq 'map({title, title_length: .title | length}) | map(select(.title_length > 60))'
```