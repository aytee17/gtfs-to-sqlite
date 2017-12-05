# gtfs-to-sqlite
A tool for generating an SQLite database from a GTFS feed. 

If your GTFS static feed contains custom fields not defined in the [GTFS specification](https://developers.google.com/transit/gtfs/reference/) they will be ignored 
when the files are parsed. To include them in the database, add the custom field to ```resources/GTFS_Specification.json``` following the conventions of the other fields.
