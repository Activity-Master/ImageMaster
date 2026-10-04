# Stateless Image Master installation

`ImageSystemInstall.update` registers Image System if it is missing from an existing
enterprise, then resolves the system and its installer token on the
provided stateless session. Image resource type creation shares the caller's
transaction and completion receipt; it does not open an independent session.
Hosts that create countries through the flag provider must prepare image and flag
taxonomy before country creation. Normal ordered updates remain idempotent.
