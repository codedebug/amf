Model: file://amf-client/shared/src/test/resources/org/raml/parser/types/multiple-inheritance-validation/input.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/types/multiple-inheritance-validation/input.raml#/declarations/types/Employee_validation
  Message: Object at / must be valid
Scalar at //age must have data type http://www.w3.org/2001/XMLSchema#integer

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/types/multiple-inheritance-validation/input.raml#/web-api/end-points/%2Fresource/get/200/application%2Fjson/schema/example/default-example
  Property: http://a.ml/vocabularies/data#age
  Position: Some(LexicalInformation([(27,0)-(29,25)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/types/multiple-inheritance-validation/input.raml