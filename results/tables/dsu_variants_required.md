| V | E | variant | avg_us | std_us | dsu_find | dsu_union | dsu_path_steps | mst_weight |
|---|---|---|---|---|---|---|---|---|
| 10 | 30 | naive (no optimisation) | 3.5 | 2.8 | 28 | 14 | 26 | 151 |
| 10 | 30 | path compression only | 2.0 | 0.8 | 28 | 14 | 21 | 151 |
| 10 | 30 | union by rank only | 0.9 | 0.1 | 28 | 14 | 19 | 151 |
| 10 | 30 | both | 0.8 | 0.0 | 28 | 14 | 17 | 151 |
| 100 | 500 | naive (no optimisation) | 45.1 | 10.6 | 474 | 237 | 3555 | 1294 |
| 100 | 500 | path compression only | 28.5 | 2.3 | 474 | 237 | 664 | 1294 |
| 100 | 500 | union by rank only | 27.4 | 1.1 | 474 | 237 | 611 | 1294 |
| 100 | 500 | both | 26.9 | 1.1 | 474 | 237 | 417 | 1294 |
| 1000 | 5000 | naive (no optimisation) | 1832.6 | 346.5 | 5208 | 2604 | 401772 | 12715 |
| 1000 | 5000 | path compression only | 1143.9 | 53.6 | 5208 | 2604 | 10329 | 12715 |
| 1000 | 5000 | union by rank only | 749.1 | 17.0 | 5208 | 2604 | 7258 | 12715 |
| 1000 | 5000 | both | 996.7 | 17.2 | 5208 | 2604 | 4668 | 12715 |
