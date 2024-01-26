# cssfact

Lossy compression of CSS for fun and loss (or profit)

## WTF?

This program takes CSS and outputs back smaller CSS that hopefully retains some (most) of the information in the input, but contains fewer rules than the original. Exactly how many rules it produces is configurable.

See [this blog post][0].

## Running it

You'll need Clojure (follow the [installation instructions][1]).

You will also need to build the code from [this repo][2]. Follow the instructions in the Makefile, then copy all the resulting `driver*` binaries into one directory. Also, copy the `wrapper.sh` script from this repo to that same directory.

Edit `src/cssfact/matrix.clj` and point it at the directory with all the binaries and the wrapper script.

Now, you can run:

`clojure -M:run -i some-css.css`

Tip: you can also pass a URL as an input file!

## License

This code is licensed under the [Opinionated Queer License, version 1.1][3].

 [0]: https://blog.danieljanus.pl/2024/01/26/lossy-css-compression/
 [1]: https://clojure.org/guides/install_clojure
 [2]: https://github.com/IBM/binary-matrix-factorization
 [3]: https://oql.avris.it/license?c=Daniel%20Janus
