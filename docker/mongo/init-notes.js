db = db.getSiblingDB("xpose");

if (db.notes.countDocuments() === 0) {
  db.notes.insertMany([
    {
      title: "Welcome Note",
      content: "This note is seeded from docker-compose Mongo init script",
      author: "system"
    },
    {
      title: "Real Mongo",
      content: "spring-xpose sample now runs against a real MongoDB container",
      author: "system"
    }
  ]);
}

