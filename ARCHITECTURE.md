# Architektur

## Überblick

Der esb0 ist ein Service Gateway (Enterprise Service Bus), welcher nach dem *VETRO*-Muster (Validate, Enrich, Transform, Route, Operate) arbeitet. Es handelt sich dabei um eine Java-Anwendung, die als .war-Datei auf einem Servlet Container bereitgestellt wird und zur Vermittlung zwischen verschiedenen Web Services und Protokollen eingesetzt werden kann.

## Struktur

Das Projekt ist in mehrere Pakete unterteilt, die jeweils einzelne Bereiche kapseln. Die wichtigsten sind im Folgenden beschrieben.

- **[`com.artofarc.esb.action`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/action)**: Implementierung der einzelnen Aktionen für die Verarbeitung der Service Flows.
- **[`com.artofarc.esb.context`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/context)**: Verwaltung von Thread Pool und Execution Context
- **[`com.artofarc.esb.servlet`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/servlet)**: Integration von HTTP Servlet und Web Interface
- **[`com.artofarc.esb.http`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/http)**: HTTP Client mit Load Balancing und Connection Pooling
- **[`com.artofarc.esb.jdbc`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/jdbc)**: JDBC-Integration
- **[`com.artofarc.esb.jms`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/jms)**: JMS-Integration
- **[`com.artofarc.esb.message`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/message)**: Zentrale Message-Strukturen
- **[`com.artofarc.esb.mbean`](https://github.com/materna-se/esb0/tree/master/src/main/java/com/artofarc/esb/mbean)**: JMX Management Beans für Monitoring

## Komponenten

### 1. GlobalContext

Der [`GlobalContext`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/context/GlobalContext.java#L58) verwaltet den zentralen Kontext der gesamten esb0-Instanz und koordiniert die einzelnen Komponenten. Er verwaltet Worker Pools, erstellt benötigte Factories und führt JNDI-Lookups durch. Darüber hinaus lädt und verwaltet er die Service-Definitionen aus dem Dateisystem. Die Initialisierung vom `GlobalContext`
erfolgt über den [`ESBServletContextListener`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/servlet/ESBServletContextListener.java#L29), der beim Deployment vom Servlet Container ausgeführt wird.

### 2. ConsumerPort

Der [`ConsumerPort`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/ConsumerPort.java#L33) definiert das Interface für alle eingehenden Schnittstellen des esb0 und abstrahiert die verschiedenen Protokolle. Der esb0 unterstützt die folgenden Protokolle:

#### HTTP / HTTPS ([HttpConsumer.java](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/servlet/HttpConsumer.java), [HttpOutboundAction.java](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/HttpOutboundAction.java))

Der esb0 unterstützt HTTP/1.1 und HTTP/2. Es wird das Chunked Transfer Encoding und Kompression mit gzip- und Fast Infoset unterstützt. Durch Unterstützung von CORS ([`HttpConstants.java`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/http/HttpConstants.java)) werden, falls benötigt, auch
Cross-Origin-Anfragen erlaubt. Es wird SOAP 1.1 und 1.2 ([`soap11.xsd`](https://github.com/materna-se/esb0/blob/main/src/main/xsd/soap11.xsd), [`soap12.xsd`](https://github.com/materna-se/esb0/blob/main/src/main/xsd/soap12.xsd)) inklusive MTOM / XOP Binary Attachments unterstützt.

Es ist ein intelligentes *Connection Pooling* mit Load Balancing implementiert. Dafür wird eine HTTP Endpoint Registry ([`HttpEndpointRegistry.java`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/http/HttpEndpointRegistry.java)) verwendet, die eine Verteilung der Anfragen auf verschiedene Endpunkte mit integrierter Health Check-Funktionalität (Failover und Circuit Breaker) unterstützt.

#### JMS (Java Message Service) ([com.artofarc.esb.jms](https://github.com/materna-se/esb0/tree/main/src/main/java/com/artofarc/esb/jms/))

Es werden alle gängigen JMS Provider und Consumer unterstützt.

#### JDBC ([com.artofarc.esb.jdbc](https://github.com/materna-se/esb0/tree/main/src/main/java/com/artofarc/esb/jdbc/))

Es werden alle JDBC-kompatiblen Datenbanken unterstützt. Connection Pooling erfolgt über JNDI, es werden ebenfalls Transaktionen unterstützt. Durch ein Mapping zwischen XML und JDBC ([`XML2JDBCMapper.java`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/jdbc/XML2JDBCMapper.java)) wird die bidirektionale Konvertierung und die weitere Verarbeitung innerhalb von Aktionen unterstützt.

#### Apache Kafka ([KafkaConsumerPort.java](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/KafkaConsumerPort.java), [KafkaProduceAction.java](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/KafkaProduceAction.java))

Die Erstellung von Kafka-Consumer und -Provider wird unterstützt. Das System unterstützt Topic-basierte Verarbeitung mit intelligenter Verwaltung der Offsets und parallele Verarbeitung auf mehreren Partitionen.

#### SMTP ([SendMailAction.java](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SendMailAction.java))

Der Versand von E-Mails wird ebenfalls unterstützt und basiert auf der Jakarta Mail API. Dabei werden auch MIME Multipart Messages und Anhänge unterstützt.

#### Dateisystem ([FileAction.java](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/FileAction.java))

Es werden Operationen auf dem Dateisystem unterstützt, unter anderem können Dateien gelesen und geschrieben werden. Es können ebenfalls Ordner erstellt und gelöscht werden.

### 3. Action Framework

Das **Action Framework** ([`Action.java`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/Action.java#L37)) bildet den Mittelpunkt der Nachrichtenverarbeitung und ermöglicht eine pipeline-basierte Verkettung von verschiedenen
Aktionen. Die einzelnen Aktionen implementieren sowohl Bestandteile vom *VETRO*-Muster als auch weitergehende Schritte. Durch die Kombination der einzelnen Aktionen können komplexe *Service Flows*
erstellt werden. Es werden darüber hinaus auch verschachtelte und bedingte Aktionen sowie die Fehlerbehandlung unterstützt. Es gibt insgesamt 42 Aktionen.

### 4. Service Flow Deployment

Die Definition der Service Flows erfolgt über XML und folgen einem definierten XML-Schema ([`service.xsd`](https://github.com/materna-se/esb0/blob/main/src/main/xsd/service.xsd)). Die Service Flows werden im `esb_root` Verzeichnis abgelegt. Der esb0
unterstützt darüber hinaus auch Hot Deployment, wodurch Service Flows zur Laufzeit geändert werden können, ohne, dass ein Neustart erforderlich ist. Diese Funktionalität wird durch den [`DeployHelper`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/artifact/DeployHelper.java) bereitgestellt. Darüber hinaus existiert unter `/admin` eine Admin-Schnittstelle, wodurch die Service Flows über HTTP verwaltet werden
können.

## Nachrichtenfluss

Die beschriebenen Komponenten erzeugen der folgende Nachrichtenfluss:
```
Eingang (HTTP, JMS, Kafka, ...) -> Routing -> Action Framework -> Ausgang
```

1. **Eingang**: Die Nachricht wird über eine Schnittstelle empfangen.
2. **Routing**: Basierend auf URL / Topic wird der relevante Service-Flow ausgewählt.
3. **Verarbeitung**: Die einzelnen Aktionen werden sequenziell abgearbeitet.
4. **Ausgang**: Die verarbeitete Nachricht wird an das Ziel weitergeleitet.

Die einzelnen Nachrichten werden als [`ESBMessage`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/message/ESBMessage.java) durch den esb0 geleitet. Die Klasse bildet die zentrale Nachrichtenstruktur und kapselt alle für die
Verarbeitung relevanten Informationen.

## Verfügbare Aktionen

### Transformation & Validation
- **[`TransformAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/TransformAction.java)**: XSLT-Transformation für XML-Nachrichten
- **[`XSLTAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/XSLTAction.java)**: XSLT-Verarbeitung
- **[`ValidateAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/ValidateAction.java)**: Validierung basierend auf XML Schema (XSD)
- **[`SAXValidationAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SAXValidationAction.java)**: SAX-basierte XML-Validierung
- **[`Json2XMLAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/Json2XMLAction.java)**: Konvertierung von JSON zu XML
- **[`XML2JsonAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/XML2JsonAction.java)**: Konvertierung von XML zu JSON
- **[`ProcessJsonAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/ProcessJsonAction.java)**: JSON-Verarbeitung mit JSON Pointer

### Protokoll & Kommunikation
- **[`HttpOutboundAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/HttpOutboundAction.java)**: HTTP- / HTTPS-Aufrufe an externe Schnittstellen
- **[`HttpInboundAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/HttpInboundAction.java)**: Verarbeitung von HTTP Request
- **[`PreSOAPHttpAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/PreSOAPHttpAction.java)**: SOAP-spezifische HTTP Vorbereitung
- **[`PostSOAPHttpAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/PostSOAPHttpAction.java)**: SOAP-spezifische HTTP Nachbearbeitung
- **[`UnwrapSOAPAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/UnwrapSOAPAction.java)**: SOAP Envelope entfernen
- **[`WrapSOAPAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/WrapSOAPAction.java)**: SOAP Envelope hinzufügen
- **[`JMSAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/JMSAction.java)**: JMS Nachrichten senden / empfangen
- **[`KafkaProduceAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/KafkaProduceAction.java)**: Kafka-Nachrichten produzieren
- **[`SendMailAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SendMailAction.java)**: E-Mail-Versand über SMTP

### Datenbank-Integration
- **[`JDBCSQLAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/JDBCSQLAction.java)**: SQL-Abfragen
- **[`JDBCProcedureAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/JDBCProcedureAction.java)**: Aufruf von Stored Procedure

### Flow Control & Logic
- **[`ConditionalAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/ConditionalAction.java)**: Bedingte Ausführung basierend auf XPath
- **[`BranchOnPathAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/BranchOnPathAction.java)**: Routing basierend auf Inhalten der Nachricht
- **[`BranchOnVariableAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/BranchOnVariableAction.java)**: Routing basierend auf Variablen
- **[`ForkAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/ForkAction.java)**: Parallele Verarbeitung von Nachrichten
- **[`IterateAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/IterateAction.java)**: Schleifen über XML-Elemente oder JSON-Arrays
- **[`SpawnAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SpawnAction.java)**: Asynchrone Verarbeitung starten
- **[`SuspendAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SuspendAction.java)**: Verarbeitung pausieren
- **[`ResumeAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/ResumeAction.java)**: Pausierte Verarbeitung fortsetzen

### Daten & Cache Management
- **[`AssignAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/AssignAction.java)**: Variablen setzen und Expressions evaluieren
- **[`SetMessageAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SetMessageAction.java)**: Nachricht modifizieren
- **[`CacheAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/CacheAction.java)**: Daten in Cache speichern
- **[`UncacheAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/UncacheAction.java)**: Cache-Einträge entfernen
- **[`DumpAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/DumpAction.java)**: Debug-Ausgabe von Nachrichten

### Datei-Operationen
- **[`FileAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/FileAction.java)**: Dateien lesen, schreiben, verschieben

### Utility
- **[`TerminalAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/TerminalAction.java)**: Ausführung von Service-Flow beenden
- **[`ThrowExceptionAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/ThrowExceptionAction.java)**: Exception werfen für Fehlerbehandlung
- **[`XOPSerializeAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/XOPSerializeAction.java)**: MTOM- / XOP- Serialisierung
- **[`XOPDeserializeAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/XOPDeserializeAction.java)**: MTOM- / XOP- Deserialisierung
- **[`SAXAction`](https://github.com/materna-se/esb0/blob/main/src/main/java/com/artofarc/esb/action/SAXAction.java)**: SAX-basierte XML-Verarbeitung

## Admin Interface

- **URL**: `http://localhost:8080/esb0/admin`
- **Authentifizierung**: HTTP Basic Auth (Rolle: `esb0admin`)
- **REST-Schnittstelle**:
    - **Service-Deployment**: POST/PUT `/admin/deploy/{service}`
    - **Service-Download**: GET `/admin/deploy/{service}`
    - **Service-Löschung**: DELETE `/admin/deploy/{service}`
    - **System-Status**: GET `/admin/status`
- **Web Interface**:
    - Darstellung und Verwaltung von Service Flows
    - Darstellung von Performance-Metriken
    - Cache-Verwaltung

## Installation
1. **`esb_root`-Ordner**: Verzeichnis für Service Flows erstellen,
    - standardmäßig: `~/esb_root`,
    - konfigurierbar über System Property `esb0.root` oder Umgebungsvariable `ESB_ROOT_DIR`,
2. **Deployment der .war-Datei**: `esb0.war` in einem Servlet Container deployen,
3. **Security**: Nuter mit Rolle `esb0admin` erstellen, um auf das Admin Interface zugreifen zu können.
