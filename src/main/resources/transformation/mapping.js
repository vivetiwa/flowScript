function mapping(records) {
    const recordsObj = JSON.parse(records);
    const activities = recordsObj.data.activity;

    let transformations = [];
    activities.forEach(activity => {
        transformations.push({...recordsObj, data : {...recordsObj.data, activity}});
    });

    console.log(JSON.stringify(transformations));
    return JSON.stringify(transformations);
}
