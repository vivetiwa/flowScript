function mapping(records) {
    const recordsObj = JSON.parse(records);
    const activities = recordsObj.data.activity;

    let transformations = [];
    activities.forEach(activity => {
        transformations.push({...recordsObj, data : {...recordsObj.data, activity}});
    });

    httpRequester.httpGetRequest("https://raw.githubusercontent.com/vivetiwa/flowScript/main/src/main/resources/sample.json");

    console.log(JSON.stringify(transformations));
    return transformations;
}