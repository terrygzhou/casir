Cost Aware Service Inconsistenc Recovery

This was a research of distributed software architecture to resove the issues of inconsistency when services are running in distributed environments where service specifications may be violated.  The inconsistency maybe caused by concurrency and observed when a later synchronisation happens, or service failures due to network, human errors etc. The violation can reduce or impact the reliability and cost. 
To fix/recovery from the inconsistency, we provide a cost-aware approach to select a "cheaper" solution in terms of cost modelling metric. 

The approach is summarised as below:
- using PCTL to describe service specification
- Using LTS and extended PLTS to model probabilistic behaviour of distributed services with their composition (based on PFS || operator)
- Using Markov chain to model service global behaviour derived from the global model produced by PLTS
- apply PCTL model checking on the generated Markov chain to assess the reliabilty and costs of the recovery solution for decision making.
 
