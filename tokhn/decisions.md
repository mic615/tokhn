Design and architecture:
* Use SHA-512/256 for hashes
* Use Prime192v1 for ECDSA curve
* Mining reward is greater of 1 or Log10(unique addresses in chain)
* No transaction fee
* No block size limit
* Built-in charity address
* Left over TXIs not tied to TXOs are given to charity
* New block every 10 minutes (configurable by Network)

Implementation:
* Should LocalBlock keep up with unique addresses to trade disk space for computation?
* The various uses of Streams should be checked for robustness and speed
