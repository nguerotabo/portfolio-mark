# The same fraction of every lot

## Context

A sale names a symbol and a share count. It does not name a lot. One symbol can have several lots, each from its own purchase, and each purchase has a matching fund lot bought with the same dollars on the same date. The replay has to decide which shares the sale consumes, on your account and on the benchmark.

## Decision

The fraction is the shares sold divided by the shares of that symbol held just before the sale, in sort order. Every lot of that symbol sells that fraction. Every fund lot that came from one of those purchases sells the same fraction, at that fund lot's own price. Proceeds are cash and earn nothing.

On your account, the sale's cash is split across that symbol's lots in proportion to the shares each lot sold. A sale never consumes one chosen lot. Selling more shares than are held refuses the run and names the symbol and the date.

Lots stay separate. They are not merged into one average holding. In the walkthrough, the 1 June sale of 50 of the 100 shares of Stock A is one half, so the benchmark sells 20 of the 40 fund shares that those dollars bought.

This is the same idea as the Canadian average cost of identical shares: a sale does not pick which identical shares were the ones sold.

## Alternatives

Taking the oldest lot first lost. The activities file does not say the oldest shares were the ones sold. The benchmark would then sell the oldest fund lot whole, and a newer fund lot would stay, so cash and sales would depend on a lot the file never chose.

Letting the reader name a lot lost. The CSV has no lot id, and this version has no place to add one.

Merging a symbol's purchases into one pooled lot lost. Each purchase keeps its own date. That date decides which fund close the benchmark used, and which distributions that fund lot was paid. A pool would erase those dates.

## Cost

A partial sale always trims every lot of that symbol, including lots bought much later. There is no way to record that one particular purchase was the one sold. Each lot is rounded once, so more lots mean more room for the account total to sit up to half a cent per lot away from one exact rounding. The bridge and the pick impacts still add exactly, because they are sums of those same cents.
