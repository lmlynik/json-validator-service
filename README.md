### JSON Validator Service

Simple service attempting to implement spec from [here](https://gist.github.com/goodits/20818f6ded767bca465a7c674187223e)

I wanted to test ZIO 2, ZIO HTTP, Tapir and Scala 3 in this exercise.

## Running

```shell
sbt run
```

### Manual testing

#### OpenApi

Tapir provides the OpenAPI docs, so one can explore the API navigating to http://localhost:8080/docs

#### Rest Client

Intellij and VSCode users can use the RestClient([for VSCode](https://marketplace.visualstudio.com/items?itemName=humao.rest-client))
on [this file](./json-validator.http)