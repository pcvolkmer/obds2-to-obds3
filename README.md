# obds2-to-obds3

[![OpenSSF Scorecard](https://img.shields.io/ossf-scorecard/github.com/bzkf/obds2-to-obds3?label=openssf%20scorecard&style=flat)](https://scorecard.dev/viewer/?uri=github.com/bzkf/obds2-to-obds3)

Library to map oBDS v2 (ADT_GEKID) into oBDS v3 messages

## Related application

Besides the library, an application can be used to map a set of messages into oBDS v3 messages.

```console
> java -jar obds2-to-obds3-app.jar

usage: java -jar obds2-to-obds3-app.jar --input <input file> --output
            <output file>
    --fix-missing-id      Fix missing IDs by generating hash values
 -i,--input <input>       Input file
    --ignore-unmappable   Ignore unmappable messages and patients
 -o,--output <output>     Output file
 -v                       Show errors
 -vv                      Show exceptions and stack traces
```
