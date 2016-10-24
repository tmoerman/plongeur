# Fast KNN performance measurements

| k(NN) | signature length | nr hash tables | block size | % of total data set | total time    | Accuracy | sampled     | date       | comments |
| ---   | ---              | ---            | ---        | ---                 | ---           | ---      | ---         | ---        | ---      |
| 10    | 20               | 1              | 200        | 10%                 | 154s          | 100%     | 5%          | 2016-10-24 |          |
| 10    | 20               | 1              | 200        | 10%                 | 247s          | 100%     | 5%          | 2016-10-24 |          |
| 10    | 20               | 5              | 200        | 10%                 | 1020s (17min) | 84%      | 5%          | 2016-10-24 |          |

* fastACC computed in 154 secs with accuracy 1.0
* fastACC computed in 247 secs with accuracy 1.0
* fastACC computed in 1020 secs with accuracy 0.8439560439560436
