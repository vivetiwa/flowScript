#include <graalvm/llvm/polyglot.h>

void *mapping(void* json) {
    void* key = polyglot_from_string("newKey", "ascii");
    void* value = polyglot_from_string("newValue", "ascii");
    polyglot_invoke(json, "put", key, value);
    return json;
}

