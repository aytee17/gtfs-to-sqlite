# gtfs-to-sqlite
A tool for generating an [SQLite](https://www.sqlite.org/about.html) database from a [GTFS](http://gtfs.org/) feed. 

## Features
* **Fast**:
Creates ~100,000 rows/second on an Apple SSD 0128F

* **Agnostic**:
Information will not be added, modified or removed when generating the database.

## Requirements
[Java Runtime Environment 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) or higher.

## Installation
### macOS
Using [Homebrew](https://brew.sh/):
```Command Language
brew tap aytee17/homebrew-tap
brew install gtsql
```

## Usage

```
usage: gtsql -p <gtfs_path> [-u <gtfs_url>] -d <database_path>
 -p,--path <gtfs_path>           Path to the GTFS data (.zip or directory)
 -u,--url <gtfs_url>             HTTP URL to the GTFS data
 -d,--database <database_path>   Path to the database file
 ```

## Notes
* If your GTFS static feed contains custom fields not defined in the [GTFS specification](http://gtfs.org/reference/) they will be ignored when the files are parsed. To include them in the database, add the custom field to ```resources/GTFS_Specification.json``` following the conventions of the other fields.
* If the feed you are working with uses ```calendar_dates.txt``` instead of ```calendar.txt```, update ```resources/GTFS_Specification.json``` to reflect this in all foreign key references to ```calendar```.
