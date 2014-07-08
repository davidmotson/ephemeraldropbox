EphemeralDropbox
================

A 5 hour project with awful UX

servlets are in /src

front end is in /WebContent

database info is in /WebContent/META-INF/context.xml

things to fix: database can keep track of timers much more efficiently than java for large numbers of users.
storing a mysql time object and querying the database every 5 seconds or so to delete all less than current time might be much more efficient. 
