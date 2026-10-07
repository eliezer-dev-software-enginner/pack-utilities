# pack-utilities

Biblioteca modular (`pack.utilities`), compilada com JDK 21.

Publique alterações para testar nos aplicativos consumidores:

```sh
./gradlew test publishToMavenLocal
```

No Windows, use `./gradlew.bat test publishToMavenLocal`.
Os consumidores devem declarar `mavenLocal()` antes dos repositórios remotos e
`implementation("pack.utilities:pack-utilities:1.0.0")`.
Depois de republicar a mesma versão, execute novamente o build do consumidor.
O artefato inclui `module-info.class` e não depende do JitPack.
