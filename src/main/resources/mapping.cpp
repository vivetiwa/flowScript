#include <iostream>
#include <iterator>
#include <map>

using namespace std;
extern "C" std::map<int, int> mapping(std::map<int, int> &json) {
    return json;
}

