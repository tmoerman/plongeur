# Fast KNN performance measurements

### Cosine distance
| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |    
| 10    | 20                | 1             | 200           | 0.1           | 13s           | 0.10      | 0.25      | 2016-10-24                    |   |
| 10    | 20                | 5             | 200           | 0.1           | 42s           | 0.44      | 0.25      | 2016-10-24                    |   |
| 10    | 20                | 10            | 200           | 0.1           | 89s           | 0.69      | 0.25      | 2016-10-24                    |   |
| 10    | 20                | 15            | 200           | 0.1           | 119s          | 0.81      | 0.25      | 2016-10-24                    |   |
| 10    | 20                | 20            | 200           | 0.1           | 128s          | 0.85      | 0.25      | 2016-10-24                    |   |
| 10    | 20                | 1             | 200           | 0.5           | 40s           | 0.025     | 0.01      | 2016-10-24                    |   |
| 10    | 20                | 1             | 200           | 0.5           | 43s           | 0.019     | 0.01      | 2016-10-24                    |   |
| 10    | 20                | 1             | 200           | 0.25          | 17s           | 0.044     | 0.05      | 2016-10-25T12:56:05.715+02:00 |   |
| 10    | 20                | 5             | 200           | 0.25          | 72s           | 0.204     | 0.05      | 2016-10-25T12:57:17.996+02:00 |   |
| 10    | 20                | 10            | 200           | 0.25          | 163s          | 0.360     | 0.05      | 2016-10-25T13:00:00.467+02:00 |   |
| 10    | 20                | 15            | 200           | 0.25          | 245s          | 0.487     | 0.05      | 2016-10-25T13:04:06           |   |

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |    
| 10    | 20                | 1             | 200           | 0.1           | 7s            | 0.106     | 0.25      | 2016-10-25T13:08:30.360+02:00 | `HashPartitioner`|
| 10    | 20                | 5             | 200           | 0.1           | 29s           | 0.444     | 0.25      | 2016-10-25T13:08:59.980+02:00 |   |
| 10    | 20                | 10            | 200           | 0.1           | 58s           | 0.700     | 0.25      | 2016-10-25T13:09:58.055+02:00 |   |
| 10    | 20                | 15            | 200           | 0.1           | 88s           | 0.813     | 0.25      | 2016-10-25T13:11:26.324+02:00 |   |
| 10    | 20                | 20            | 200           | 0.1           | 115s          | 0.854     | 0.25      | 2016-10-25T13:13:21.335+02:00 |   |

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |    
| 10    | 20                | 1             | 50            | 0.1           | 5s            | 0.022     | 0.25      | 2016-10-25T13:18:47.104+02:00 |   | 
| 10    | 20                | 5             | 50            | 0.1           | 12s           | 0.118     | 0.25      | 2016-10-25T13:18:59.474+02:00 |   |
| 10    | 20                | 10            | 50            | 0.1           | 20s           | 0.226     | 0.25      | 2016-10-25T13:19:20.086+02:00 |   | 
| 10    | 20                | 15            | 50            | 0.1           | 31s           | 0.328     | 0.25      | 2016-10-25T13:19:51.818+02:00 |   |
| 10    | 20                | 20            | 50            | 0.1           | 39s           | 0.409     | 0.25      | 2016-10-25T13:20:30.957+02:00 |   |
| 10    | 20                | 25            | 50            | 0.1           | 47s           | 0.479     | 0.25      | 2016-10-25T13:21:18.253+02:00 |   |

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |    
| 10    | 20                | 1             | 100           | 0.1           | 5s            | 0.051     | 0.25      | 2016-10-25T14:21:59.798+02:00 |   | 
| 10    | 20                | 5             | 100           | 0.1           | 19s           | 0.240     | 0.25      | 2016-10-25T14:22:19.026+02:00 |   | 
| 10    | 20                | 10            | 100           | 0.1           | 34s           | 0.426     | 0.25      | 2016-10-25T14:22:53.964+02:00 |   | 
| 10    | 20                | 15            | 100           | 0.1           | 54s           | 0.560     | 0.25      | 2016-10-25T14:23:48.746+02:00 |   | 
| 10    | 20                | 20            | 100           | 0.1           | 70s           | 0.665     | 0.25      | 2016-10-25T14:24:59.283+02:00 |   | 
| 10    | 20                | 25            | 100           | 0.1           | 89s           | 0.739     | 0.25      | 2016-10-25T14:26:28.863+02:00 |   | 

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |     
| 10    | 20                | 1             | 100           | 0.1           | 4s            | 0.054     | 0.25      | 2016-10-25T14:51:25.380+02:00 | combine1  |
| 10    | 20                | 5             | 100           | 0.1           | 16s           | 0.240     | 0.25      | 2016-10-25T14:51:41.690+02:00 |   |
| 10    | 20                | 10            | 100           | 0.1           | 31s           | 0.417     | 0.25      | 2016-10-25T14:52:12.721+02:00 |   |
| 10    | 20                | 15            | 100           | 0.1           | 53s           | 0.555     | 0.25      | 2016-10-25T14:53:06.109+02:00 |   |
| 10    | 20                | 20            | 100           | 0.1           | 63s           | 0.653     | 0.25      | 2016-10-25T14:54:09.191+02:00 |   |
| 10    | 20                | 25            | 100           | 0.1           | 83s           | 0.728     | 0.25      | 2016-10-25T14:55:32.508+02:00 |   |

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |
| 10    | 20                | 1             | 100           | 0.1           | 5s            | 0.054     | 0.25      | 2016-10-25T15:05:04.553+02:00 | combine1 stream | 
| 10    | 20                | 5             | 100           | 0.1           | 20s           | 0.230     | 0.25      | 2016-10-25T15:05:24.850+02:00 | | 
| 10    | 20                | 10            | 100           | 0.1           | 38s           | 0.411     | 0.25      | 2016-10-25T15:06:03.017+02:00 | | 
| 10    | 20                | 15            | 100           | 0.1           | 54s           | 0.549     | 0.25      | 2016-10-25T15:06:57.957+02:00 | | 
| 10    | 20                | 20            | 100           | 0.1           | 83s           | 0.648     | 0.25      | 2016-10-25T15:08:21.170+02:00 | | 
| 10    | 20                | 25            | 100           | 0.1           | 99s           | 0.723     | 0.25      | 2016-10-25T15:10:00.822+02:00 | |

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- | 
| 10    | 20                | 1             | 100           | 0.1           | 4s            | 0.062     | 0.25      | 2016-10-25T15:12:49.138+02:00 | combine2   | 
| 10    | 20                | 5             | 100           | 0.1           | 18s           | 0.250     | 0.25      | 2016-10-25T15:13:08.155+02:00 |    | 
| 10    | 20                | 10            | 100           | 0.1           | 37s           | 0.427     | 0.25      | 2016-10-25T15:13:45.798+02:00 |    | 
| 10    | 20                | 15            | 100           | 0.1           | 55s           | 0.562     | 0.25      | 2016-10-25T15:14:40.839+02:00 |    | 
| 10    | 20                | 20            | 100           | 0.1           | 77s           | 0.659     | 0.25      | 2016-10-25T15:15:58.807+02:00 |    | 
| 10    | 20                | 25            | 100           | 0.1           | 100s          | 0.735     | 0.25      | 2016-10-25T15:17:38.944+02:00 |    |    

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |     
| 10    | 40                | 1             | 100           | 0.1           | 5s            | 0.050     | 0.25      | 2016-10-25T15:22:22.752+02:00 |   | 
| 10    | 40                | 5             | 100           | 0.1           | 19s           | 0.233     | 0.25      | 2016-10-25T15:22:41.920+02:00 |   | 
| 10    | 40                | 10            | 100           | 0.1           | 31s           | 0.416     | 0.25      | 2016-10-25T15:23:13.905+02:00 |   | 
| 10    | 40                | 15            | 100           | 0.1           | 52s           | 0.551     | 0.25      | 2016-10-25T15:24:06.048+02:00 |   | 
| 10    | 40                | 20            | 100           | 0.1           | 70s           | 0.662     | 0.25      | 2016-10-25T15:25:16.391+02:00 |   |
| 10    | 40                | 25            | 100           | 0.1           | 97s           | 0.735     | 0.25      | 2016-10-25T15:26:53.597+02:00 |   | 

| k(NN) | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |     
| 10    | 10                | 1             | 100           | 0.1           | 5s            | 0.056     | 0.25      | 2016-10-25T15:46:47.409+02:00 |   | 
| 10    | 10                | 5             | 100           | 0.1           | 19s           | 0.239     | 0.25      | 2016-10-25T15:47:07.038+02:00 |   | 
| 10    | 10                | 10            | 100           | 0.1           | 36s           | 0.412     | 0.25      | 2016-10-25T15:47:43.961+02:00 |   | 
| 10    | 10                | 15            | 100           | 0.1           | 55s           | 0.558     | 0.25      | 2016-10-25T15:48:39.378+02:00 |   | 
| 10    | 10                | 20            | 100           | 0.1           | 72s           | 0.661     | 0.25      | 2016-10-25T15:49:51.742+02:00 |   | 
| 10    | 10                | 25            | 100           | 0.1           | 92s           | 0.737     | 0.25      | 2016-10-25T15:51:24.020+02:00 |   |

| k(NN) | distance          | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments |
| ---   | ---               | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | --- |     
| 10    | Cosine            | 10                | 30            | 100           | 1.0           | 1088s         | 0.239     | 0.025     | 2016-10-25T16:19:25.591+02:00 |   | 
| 10    | Cosine            | 50                | 30            | 100           | 1.0           | 899s          | 0.162     | 0.025     | 2016-10-25T22:00:01.057+02:00 |   | 
| 10    | Manhattan         | 50                | 30            | 100           | 1.0           | 688s          | 0.193     | 0.025     | 2016-10-25T22:23:39.959+02:00 |   |

# FastKNN2

| k(NN) | distance  | signature length  | # hash tables | block size    | part of total | total time    | Accuracy  | sampled   | date                          | Comments  |
| ---   | ---       | ---               | ---           | ---           | ---           | ---           | ---       | ---       | ---                           | ---       |
| 10    | Manhattan | 10                | 1             | 100           | 0.1           | 3s            | 0.052     | 0.25      | 2016-10-26T11:33:41.443+02:00 |           | 
| 10    | Manhattan | 10                | 5             | 100           | 0.1           | 12s           | 0.256     | 0.25      | 2016-10-26T11:33:53.800+02:00 |           | 
| 10    | Manhattan | 10                | 10            | 100           | 0.1           | 24s           | 0.459     | 0.25      | 2016-10-26T11:34:18.511+02:00 |           | 
| 10    | Manhattan | 10                | 15            | 100           | 0.1           | 37s           | 0.605     | 0.25      | 2016-10-26T11:34:55.869+02:00 |           | 
| 10    | Manhattan | 10                | 20            | 100           | 0.1           | 50s           | 0.707     | 0.25      | 2016-10-26T11:35:46.107+02:00 |           | 
| 10    | Manhattan | 10                | 25            | 100           | 0.1           | 63s           | 0.782     | 0.25      | 2016-10-26T11:36:50.039+02:00 |           | 
| 10    | Cosine    | 10                | 1             | 100           | 0.1           | 4s            | 0.057     | 0.25      | 2016-10-26T11:40:07.330+02:00 |           | 
| 10    | Cosine    | 10                | 5             | 100           | 0.1           | 15s           | 0.261     | 0.25      | 2016-10-26T11:40:23.290+02:00 |           | 
| 10    | Cosine    | 10                | 10            | 100           | 0.1           | 34s           | 0.454     | 0.25      | 2016-10-26T11:40:58.274+02:00 |           | 
| 10    | Cosine    | 10                | 15            | 100           | 0.1           | 45s           | 0.583     | 0.25      | 2016-10-26T11:41:44.205+02:00 |           | 
| 10    | Cosine    | 10                | 20            | 100           | 0.1           | 67s           | 0.684     | 0.25      | 2016-10-26T11:42:52.170+02:00 |           | 
| 10    | Cosine    | 10                | 25            | 100           | 0.1           | 90s           | 0.757     | 0.25      | 2016-10-26T11:44:22.483+02:00 |           | 
| 10    | Cosine    | 40                | 1             | 100           | 0.1           | 6s            | 0.052     | 0.25      | 2016-10-26T11:47:21.729+02:00 |           | 
| 10    | Cosine    | 40                | 5             | 100           | 0.1           | 23s           | 0.252     | 0.25      | 2016-10-26T11:47:45.469+02:00 |           | 
| 10    | Cosine    | 40                | 10            | 100           | 0.1           | 50s           | 0.444     | 0.25      | 2016-10-26T11:48:35.484+02:00 |           | 
| 10    | Cosine    | 40                | 15            | 100           | 0.1           | 72s           | 0.581     | 0.25      | 2016-10-26T11:49:48.315+02:00 |           | 
| 10    | Cosine    | 40                | 20            | 100           | 0.1           | 101s          | 0.687     | 0.25      | 2016-10-26T11:51:29.940+02:00 |           | 
| 10    | Cosine    | 40                | 25            | 100           | 0.1           | 116s          | 0.761     | 0.25      | 2016-10-26T11:53:26.931+02:00 |           | 
| 10    | Cosine    | 40                | 30            | 100           | 0.1           | __4s__        | 0.802     | 0.25      | 2016-10-26T11:53:26.931+02:00 |           |
| 10    | Cosine    | 40                | 30            | 50            | 0.1           | 60s           | 0.551     | 0.25      | 2016-10-26T14:59:56.434+02:00 |           |
| 10    | Cosine    | 40                | 60            | 50            | 0.1           | 137s          | 0.772     | 0.25      | 2016-10-26T15:04:34.134+02:00 |           | 
| 10    | Cosine    | 40                | 30            | 100           | 0.1           | 120s          | 0.784     | 0.25      | 2016-10-26T14:45:36.384+02:00 |           |
| 10    | Cosine    | 40                | 30            | 200           | 0.1           | 260s          | 0.877     | 0.25      | 2016-10-26T14:53:42.533+02:00 |           |
| 10    | Cosine    | 40                | 60            | 50            | 1.0           | 1315s         | 0.162     | Left(250) | 2016-10-26T15:33:32.182+02:00 |           |  
| 10 | CosineDistance    | 40 | 30 | 100 | 1.0 | 7s | 0.161 | Right(0.05) | 2016-10-26T17:54:50.344+02:00 | |
| 10 | ManhattanDistance | 50 | 30 | 100 | 0.1 | 1s | 0.827 | Right(0.05) | 2016-10-26T18:10:16.101+02:00 | |
| 10 | ManhattanDistance | 50 | 30 | 100 | 0.5 | 4s | 0.328 | Right(0.05) | 2016-10-28T10:40:10.671+02:00 | | 

| k(NN)=10 | ManhattanDistance | r=3.5  | sig=50 | L=30 | B=100 | 0.1  | 1s | 0.786 | Right(0.25) | 2016-10-28T12:13:24.505+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=50 | L=60 | B=100 | 0.25 | 11m | 0.772 | Right(0.1) | 2016-10-28T13:22:42.625+02:00  | |

 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=100 | L=10 | B=100 | 0.1 | 42s | 0.450 | Right(0.25) | 2016-10-28T13:58:37.029+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=100 | L=30 | B=100 | 0.1 | 60s | 0.815 | Right(0.25) | 2016-10-28T13:59:37.667+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=100 | L=60 | B=100 | 0.1 | 130s | 0.969 | Right(0.25) | 2016-10-28T14:01:47.836+02:00 | |
  
| k(NN)=10 | ManhattanDistance   | r=10.0 | sig=50 | L=60 | B=100 | 0.25 | 5s | 0.772 | Right(0.1) | 2016-10-28T13:22:42.625+02:00  | |
| k(NN)=10 | ManhattanDistance   | r=10.0 | sig=50 | L=60 | B=100 | 0.1  | 6m | 0.967 | Right(0.25) | 2016-10-28T13:39:05.904+02:00 | |

| k(NN)=10 | ManhattanDistance | r=10.0 | sig=25 | L=10 | B=100 | 0.1 | 33s  | 0.458 | Right(0.25) | 2016-10-28T14:09:50.014+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=25 | L=30 | B=100 | 0.1 | 48s  | 0.834 | Right(0.25) | 2016-10-28T14:10:38.076+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=25 | L=60 | B=100 | 0.1 | 102s | 0.976 | Right(0.25) | 2016-10-28T14:12:21.020+02:00 | | 

| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=10 | B=100 | 0.1 | 51s  | 0.475 | Right(0.25) | 2016-10-28T14:14:29.679+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=30 | B=100 | 0.1 | 74s  | 0.815 | Right(0.25) | 2016-10-28T14:15:44.170+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=60 | B=100 | 0.1 | 160s | 0.962 | Right(0.25) | 2016-10-28T14:18:24.610+02:00 | |
 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=10 | B=50 | 0.1 | 42s  | 0.275 | Right(0.25) | 2016-10-28T14:21:15.442+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=30 | B=50 | 0.1 | 37s  | 0.592 | Right(0.25) | 2016-10-28T14:21:52.650+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=60 | B=50 | 0.1 | 82s  | 0.833 | Right(0.25) | 2016-10-28T14:23:14.886+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=10.0 | sig=10 | L=90 | B=50 | 0.1 | 112s | 0.926 | Right(0.25) | 2016-10-28T14:32:06.919+02:00 | | 

| k(NN)=10 | ManhattanDistance | r=50.0 | sig=10 | L=10 | B=50 | 0.1 | 37s | 0.252 | Right(0.25) | 2016-10-28T15:36:59.358+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=50.0 | sig=10 | L=30 | B=50 | 0.1 | 42s | 0.595 | Right(0.25) | 2016-10-28T15:37:41.780+02:00 | | 
| k(NN)=10 | ManhattanDistance | r=50.0 | sig=10 | L=60 | B=50 | 0.1 | 92s | 0.841 | Right(0.25) | 2016-10-28T15:39:14.568+02:00 | | 

| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=10 | B=50 | 0.1 | 86s | 0.249 | Right(0.25) | 2016-10-28T15:43:45.243+02:00 | | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=30 | B=50 | 0.1 | 112s | 0.593 | Right(0.25) | 2016-10-28T15:45:37.530+02:00 | | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=60 | B=50 | 0.1 | 155s | 0.809 | Right(0.25) | 2016-10-28T15:48:12.555+02:00 | | 

| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=10 | B=50 | 0.25 | 144s | 0.122 | Right(0.1) | 2016-10-28T16:10:25.769+02:00 | | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=30 | B=50 | 0.25 | 175s | 0.299 | Right(0.1) | 2016-10-28T16:13:20.886+02:00 | |

| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=10 | B=50 | 0.25 | 144s | 0.122 | Right(0.1) | 2016-10-28T16:10:25.769+02:00 | | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=30 | B=50 | 0.25 | 175s | 0.299 | Right(0.1) | 2016-10-28T16:13:20.886+02:00 | | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=60 | B=50 | 0.25 | 446s | 0.530 | Right(0.1) | 2016-10-28T16:20:47.121+02:00 | |  


| k-NN | distance metric | radius | signature length | # hash tables | block size | fraction of total data | execution time | estimated accuracy | test fraction |
| ---  | ---             | ---    | ---              | ---           | ---        | ---                    | ---            | ---                | ---           |
| k=10 | $L_{1/2}$ | r=50.0 | sig=10 | L=10 | B=50  | 0.25 | 144s | 0.122 | 0.1 | 
| k=10 | $L_{1/2}$ | r=50.0 | sig=10 | L=30 | B=50  | 0.25 | 175s | 0.299 | 0.1 | 
| k=10 | $L_{1/2}$ | r=50.0 | sig=10 | L=60 | B=50  | 0.25 | 446s | 0.530 | 0.1 | 
| k=10 | $L_{1/2}$ | r=50.0 | sig=10 | L=10 | B=100 | 0.25 | 234s | 0.226 | 0.1 | 
| k=10 | $L_{1/2}$ | r=50.0 | sig=10 | L=30 | B=100 | 0.25 | 355s | 0.525 | 0.1 | 
| k=10 | $L_{1/2}$ | r=50.0 | sig=10 | L=60 | B=100 | 0.25 | 667s | 0.772 | 0.1 | 

| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=10 | B=100 | 0.25 | 165s | 0.219 | Right(0.1) | 2016-10-28T19:06:58.549+02:00 | Spark 2.0.1 | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=30 | B=100 | 0.25 | 270s | 0.524 | Right(0.1) | 2016-10-28T19:11:29.406+02:00 | Spark 2.0.1 | 
| k(NN)=10 | LpNormDistance(0.5) | r=50.0 | sig=10 | L=60 | B=100 | 0.25 | 697s | 0.768 | Right(0.1) | 2016-10-28T19:23:06.595+02:00 | Spark 2.0.1 | 
| k(NN)=10 | LpNormDistance(0.5) | r=10.0 | sig=10 | L=10 | B=100 | 0.25 | 133s | 0.224 | Right(0.1) | 2016-10-28T19:28:52.102+02:00 | Spark 2.0.1 | 
| k(NN)=10 | LpNormDistance(0.5) | r=10.0 | sig=10 | L=30 | B=100 | 0.25 | 249s | 0.538 | Right(0.1) | 2016-10-28T19:33:01.376+02:00 | Spark 2.0.1 | 
| k(NN)=10 | LpNormDistance(0.5) | r=10.0 | sig=10 | L=60 | B=100 | 0.25 | 446s | 0.777 | Right(0.1) | 2016-10-28T19:40:28.006+02:00 | Spark 2.0.1 | 