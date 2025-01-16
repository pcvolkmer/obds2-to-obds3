# obds2-to-obds3

Library to map oBDS v2 (ADT_GEKID) into oBDS v3 messages

## Related application

Besides the library, an application can be used to map a set of messages into oBDS v3 messages.

```
> java -jar obds2-to-obds3-app.jar 

usage: java -jar obds2-to-obds3-app.jar --input <input file> --output
            <output file>
 -i,--input <input>       Input file
    --ignore-unmappable   Ignore unmappable messages and patients
 -o,--output <output>     Output file
 -v                       Show errors
 -vv                      Show exceptions and stack traces
```
