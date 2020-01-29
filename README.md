# saa

Finnish Meterological Institute forecasts

## Installation

An api key used to be a requirement, but now access is open to everybody. More info at
https://en.ilmatieteenlaitos.fi/open-data-manual-accessing-data

To build the classes, do this:

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
