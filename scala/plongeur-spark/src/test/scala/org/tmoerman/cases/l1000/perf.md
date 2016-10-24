# Fast KNN performance measurements

### Fixed algorithm
| k(NN) | signature length | nr hash tables | block size | part of total    | total time    | Accuracy | sampled     | date       | comments                    |
| ---   | ---              | ---            | ---        | ---              | ---           | ---      | ---         | ---        | ---                         |
| 10    | 20               | 1              | 200        | 0.1              | 13s           | 0.10     | 0.25        | 2016-10-24 | sampled duration 53s        |
| 10    | 20               | 5              | 200        | 0.1              | 42s           | 0.44     | 0.25        | 2016-10-24 | sampled duration 27s        | 
| 10    | 20               | 10             | 200        | 0.1              | 89s           | 0.69     | 0.25        | 2016-10-24 | sampled duration 27s        |
| 10    | 20               | 15             | 200        | 0.1              | 119s          | 0.81     | 0.25        | 2016-10-24 | sampled duration 43s        | 
| 10    | 20               | 20             | 200        | 0.1              | 128s          | 0.85     | 0.25        | 2016-10-24 | sampled duration 29s        | 
| 10    | 20               | 1              | 200        | 0.5              | 40s           | 0.025    | 0.01        | 2016-10-24 | sampled duration 32s        | 
| 10    | 20               | 1              | 200        | 0.5              | 43s           | 0.019    | 0.01        | 2016-10-24 | sampled duration 26s        | 


### Old with wrong algorithm
| k(NN) | signature length | nr hash tables | block size | % of total data set | total time    | Accuracy | sampled     | date       | comments                     |
| 10    | 20               | 1              | 200        | 10%                 | 154s          | 100%     | 5%          | 2016-10-24 | error in algo                |
| 10    | 20               | 1              | 200        | 10%                 | 247s          | 100%     | 5%          | 2016-10-24 | error in algo                |
| 10    | 20               | 1              | 200        | 10%                 | 175s          | 100%     | exact       | 2016-10-24 | error in algo                |
| 10    | 20               | 5              | 200        | 10%                 | 1020s (17min) | 84%      | 5%          | 2016-10-24 | error in                     |
