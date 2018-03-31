const apple_ontology = {
    "professor": [], 
    "topic": [], 
    "course": [], 
    "journal": [], 
    "sponsor_agency": [], 
    "conference": [], 
    "conference_acronym": [],
    "money": [], 
    "email": [], 
    "phone": [], 
    "zipcode": [], 
    "date": [], 
    "year": [], 
    "course_number": [], 
    "number": []
 }

const entity_types = Object.keys(apple_ontology);

var suggestions = [];

for( var i = 0; i < entity_types.length; i++ ){
    var temp = {
        label: "#"+entity_types[i].substr(0).split(' ').join('_').toLowerCase()
    }
    suggestions.push(temp)
}


export default suggestions;
