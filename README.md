# scavenger

FIXME: description

## Requirements

`leiningen` is required for running and compiling the project.

## Installation

Install clojure dependencies with:

    $ lein deps

## Usage

Start the development API server with:

    $ lein ring server

Currently the server only outputs a static response on http://localhost:3000/
but will be expanded to serve API responses for the client app.

Start interactive REPL for client side app with:

    $ lein run -m clojure.main script/figwheel.clj

The application will now be available at http://localhost:3449/ and connects
to a REPL on first load. Run expressions in the REPL to have them be executed
in the browser. Any changes to the code will automatically be rebuilt and
pushed to the browser.

For better readline support in the REPL, install rlwrap and run figwheel
through it:

    $ rlwrap lein run -m clojure.main script/figwheel.clj

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
