
```
for ((id=7162; id<7190; id++)); do
  curl -s -H 'Cookie: JSESSIONID=csrf' -H 'Csrf-Token: csrf'  -H 'x-restli-protocol-version: 2.0.0' "https://www.linkedin.com/learning-api/detailedCategories?categoryUrn=List(urn%3Ali%3AlyndaCategory%3A$id)&getChildCategories=true&minNumberOfCoursesPerChildCategory=3&q=urnForBrowse" | jq "." | awk '/name/ {print $2}' | tr -d '",' | tr '[A-Z]-' '[a-z] '
done | sort | uniq > LIST_OF_TOPICS
```

```
for ((id=7162; id<7190; id++)); do
  curl -s -H 'Cookie: JSESSIONID=csrf' -H 'Csrf-Token: csrf'  -H 'x-restli-protocol-version: 2.0.0' "https://www.linkedin.com/learning-api/learningListedCategories?category=List(urn%3Ali%3AlyndaCategory%3A$id)&q=associatedSoftwareByCategory" | jq "." | awk '/name/ {print $2}' | tr -d '",' | tr '[A-Z]-' '[a-z] '
done | sort | uniq > LIST_OF_SOFTWARE
```
