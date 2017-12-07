# gtfs-to-sqlite
A tool for generating an [SQLite](https://www.sqlite.org/about.html) database from a [GTFS](http://gtfs.org/) feed. 

## Features
* **Fast**:
Creates ~50000 rows/second with stop_times.txt

* **Safe**:
Checks the feed contains all required files and attributes according to the specification

* **Up to date**:
Changes to the spec does not require an update to the tool. Read the wiki for full details.

* **Agnostic**:
All data is parsed 'as is'. Information will not be added, modified or removed when generating the database. 

## Installation
### macOS
Using [Homebrew](https://brew.sh/):
```Command Language
brew install gtsql
```

## Usage

```
usage: gtsql -p <gtfs_path> [-u <gtfs_url>] -d <database_path>
 -p,--path <gtfs_path>           Path to the GTFS data
 -u,--url <gtfs_url>             URL to the GTFS data
 -d,--database <database_path>   Path to the database file
 ```
 
## Notes
If your GTFS static feed contains custom fields not defined in the [GTFS specification](http://gtfs.org/reference/) they will be ignored when the files are parsed. To include them in the database, add the custom field to ```resources/GTFS_Specification.json``` following the conventions of the other fields.

For more information consult the wiki.
