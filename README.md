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

### Possible problems

Current Schema store is file based and setup configures it for the systems `tmp` directory.
This might cause issues on systems which might have that directory restricted.

### Nice-to-haves

1. Blackbox/Integration test

   A suite of tests checking the service as a blackbox asserting correct responses for the HTTP interface

2. Better store implementation

   Its file based currently, there are better solutions. Currently, the spec forbids updates to an updated schema(or deletes) so it's pretty safe.
   
   If such requirements came up, we could use a Semaphore on the store which would potentially hinder performance or explore STM