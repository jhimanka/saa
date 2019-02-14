# saa

Finnish Meterological Institute forecasts

## Installation

You need your own api key to access the forecasts. It is free and can
be obtained from
https://ilmatieteenlaitos.fi/rekisteroityminen-avoimen-datan-kayttajaksi

Once you have the api key, in a file called secrets.edn, place the
following:

{:apikey "XXXXXXXXX"}

mkdir classes

clj -e "(compile 'saa)"

## Usage

./runsaa -l pispala,tampere -m Precipitation1h

## Options

./runsaa -h for all possible measurements

## Inspiration

I was reading Malcolm Sparks's excellent post about using transducers
to select parts of XML documents and thought it would be a good
approach for parsing the humongous XML data that the FMI produces. 

The post is here https://juxt.pro/blog/posts/xpath-in-transducers.html
