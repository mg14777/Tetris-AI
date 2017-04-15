# Todo List
## 1st Steps (before implement ML)
### PlayerSkeleton
- [X] Compute next state from current state and legal moves
- [X] Make a for loop to call the features of it
- [X] Use [those weights](https://codemyroad.wordpress.com/2013/04/14/tetris-ai-the-near-perfect-player/) for the moment in order to have a base that works

### Feature - Implement the different features
- [X] aggregateHeight (use Heights)
- [X] holes
- [X] completeLines
- [X] bumpiness
- [X] height

### Possible Evolution ???
- [ ] Use parallelism (search is highly parallelizable)
- [ ] Add a second ply to search with probability (can use generateNextState from StateWrapper in order to do that) and then weight possibly weight it ?

## 2nd Step (ML / Genetic algo)
### Machine Learning / Genetic algorithm
- [ ] Implement ML or Genetic in order to calculate the best weights
