package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.domain.Equipement;
import com.example.demo.domain.Panne;
import com.example.demo.domain.Piece;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.StatutPanne;
import com.example.demo.domain.UsagePiece;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.PanneRepository;
import com.example.demo.repository.PieceRepository;
import com.example.demo.repository.PretRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatbotService {
    private final EquipementRepository equipements;
    private final PanneRepository pannes;
    private final PieceRepository pieces;
    private final PretRepository prets;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.ai.openai.model:gpt-4.1-mini}")
    private String openAiModel;

    @Value("${app.ai.openai.url:https://api.openai.com/v1/responses}")
    private String openAiUrl;

    public ChatbotService(EquipementRepository equipements, PanneRepository pannes, PieceRepository pieces,
            PretRepository prets, DashboardService dashboardService, ObjectMapper objectMapper) {
        this.equipements = equipements;
        this.pannes = pannes;
        this.pieces = pieces;
        this.prets = prets;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    @Transactional(readOnly = true)
    public ChatReply answer(String message, Authentication authentication) {
        if (aiAvailable()) {
            try {
                return aiAnswer(message, authentication);
            } catch (RuntimeException ex) {
                ChatReply fallback = localAnswer(message, authentication);
                return withAiFailureNote(fallback, ex);
            }
        }
        return localAnswer(message, authentication);
    }

    private boolean aiAvailable() {
        return aiEnabled && openAiApiKey != null && !openAiApiKey.isBlank();
    }

    private ChatReply withAiFailureNote(ChatReply fallback, RuntimeException ex) {
        if (isQuotaError(ex)) {
            return fallback;
        }
        String reason = friendlyAiFailure(ex);
        return new ChatReply(
                fallback.title() + " (mode local)",
                fallback.answer() + " Note: " + reason + " J'ai donc repondu avec l'analyse locale.",
                fallback.bullets(),
                fallback.module()
        );
    }

    private boolean isQuotaError(RuntimeException ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        return raw.contains("insufficient_quota")
                || raw.contains("exceeded your current quota")
                || raw.contains("HTTP 429");
    }

    private String friendlyAiFailure(RuntimeException ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        if (raw.contains("insufficient_quota") || raw.contains("exceeded your current quota")
                || raw.contains("HTTP 429")) {
            return "quota OpenAI/credits insuffisants.";
        }
        if (raw.contains("HTTP 401") || raw.contains("invalid_api_key")) {
            return "cle OpenAI invalide ou expiree.";
        }
        if (raw.contains("HTTP 404") || raw.contains("model_not_found")) {
            return "modele OpenAI indisponible pour cette cle.";
        }
        return "l'appel AI externe a echoue.";
    }

    private ChatReply localAnswer(String message, Authentication authentication) {
        String query = normalize(message);
        if (query.isBlank()) {
            return help(authentication);
        }
        if (containsAny(query, "stock", "stok", "stoki")) {
            return stockAnswer(query, authentication);
        }
        if (containsAny(query, "3andi", "andi", "dyali", "mes", "mon", "ma piece", "mes pieces",
                "mon materiel", "equipement dyali", "materiel dyali")) {
            return equipementAnswer(query, authentication);
        }
        if (containsAny(query, "stock", "stok", "stoki", "piece", "pieces", "9ta3", "qta3", "قطع",
                "rupture", "critique", "commande", "cmd", "9lil", "qlil", "na9s", "naqes", "ناقس")) {
            return stockAnswer(query, authentication);
        }
        if (hasRole(authentication, "EMPLOYE")
                && containsAny(query, "declarer", "declare", "declari", "declaration", "bghit ndeclari",
                        "nsift panne", "signaler", "sift panne", "ndir panne")) {
            return declarePanneHelp();
        }
        if (containsAny(query, "panne", "pannes", "pana", "mouchkil", "mochkil", "problem", "probleme",
                "incident", "incidents", "reparation", "salah", "tsali7", "technicien", "tech",
                "action", "today", "aujourd", "lyom", "l اليوم", "شنو ندير", "chno ndir", "ach ndir",
                "khasni", "khassni", "urgent")) {
            return actionsAnswer(authentication);
        }
        if (containsAny(query, "dashboard", "resume", "résumé", "kpi", "situation", "etat", "global",
                "parc", "bilan", "rapport", "chno kayn", "شنو كاين", "fin wsalna")) {
            return dashboardAnswer(authentication);
        }
        if (containsAny(query, "equipement", "equipment", "materiel", "matriel", "pc", "ordinateur",
                "laptop", "serie", "serial", "affecte", "3andi", "andi", "mon materiel", "فين",
                "fin kayn", "wach 3andi")) {
            return equipementAnswer(query, authentication);
        }
        if (containsAny(query, "pack", "pak", "ai", "profil", "profile", "developpeur", "developer",
                "finance", "direction", "support", "employe", "employer", "khddam", "integration")) {
            return packAnswer(query, authentication);
        }
        return smartFallback(query, authentication);
    }

    private ChatReply aiAnswer(String message, Authentication authentication) {
        String context = buildAiContext(authentication);
        String roleGuide = roleGuide(authentication);
        String prompt = """
                Tu es PT17 Copilot, assistant AI integre a une application de gestion de parc informatique.

                Regles importantes:
                - Reponds en francais simple avec quelques mots darija si l'utilisateur ecrit en darija.
                - Utilise uniquement le contexte fourni ci-dessous comme source de verite.
                - Si une information n'existe pas dans le contexte, dis-le clairement.
                - Adapte la reponse au role exact de l'utilisateur.
                - Ne donne jamais de SQL destructif.
                - Retourne uniquement un JSON valide, sans markdown.

                Guide role:
                %s

                Format obligatoire:
                {
                  "title": "titre court",
                  "answer": "reponse courte et claire",
                  "bullets": ["point 1", "point 2", "point 3"],
                  "module": "DASHBOARD|STOCK|PANNES|EQUIPEMENTS|PACK|HELP"
                }

                Contexte application:
                %s

                Question utilisateur:
                %s
                """.formatted(roleGuide, context, message == null ? "" : message);

        try {
            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "input", prompt,
                    "max_output_tokens", 700
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openAiUrl))
                    .timeout(Duration.ofSeconds(35))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI HTTP " + response.statusCode() + ": "
                        + summarizeOpenAiError(response.body()));
            }
            String text = extractResponseText(response.body());
            return parseAiReply(text);
        } catch (IOException ex) {
            throw new IllegalStateException("AI response parse failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI request interrupted", ex);
        }
    }

    private String buildAiContext(Authentication authentication) {
        List<Panne> visiblePannes = visiblePannes(authentication).stream()
                .filter(panne -> panne.getStatut() != StatutPanne.CLOTUREE)
                .sorted(Comparator.comparing(Panne::getDateDeclaration, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();
        boolean employe = hasRole(authentication, "EMPLOYE");
        boolean technicien = hasRole(authentication, "TECHNICIEN");
        boolean globalRole = hasRole(authentication, "ADMIN") || hasRole(authentication, "DIRECTEUR");
        List<Piece> stockCritique = employe
                ? List.of()
                : pieces.findAll().stream()
                        .filter(Piece::stockCritique)
                        .sorted(Comparator.comparingInt(Piece::getQuantiteStock))
                        .limit(10)
                        .toList();
        List<Equipement> visibleEquipements = employe
                ? equipements.findByUtilisateurLogin(authentication.getName())
                : equipements.findAll();
        long disponibles = visibleEquipements.stream().filter(e -> e.getStatut() == StatutEquipement.DISPONIBLE).count();
        long affectes = visibleEquipements.stream().filter(e -> e.getStatut() == StatutEquipement.AFFECTE).count();
        long enPanne = visibleEquipements.stream().filter(e -> e.getStatut() == StatutEquipement.EN_PANNE).count();

        StringBuilder builder = new StringBuilder();
        builder.append("Utilisateur: ").append(authentication == null ? "anonyme" : authentication.getName())
                .append(" | roles: ").append(authentication == null ? "-" : authentication.getAuthorities()).append('\n');
        if (globalRole) {
            Map<String, Object> kpis = dashboardService.kpis();
            builder.append("KPIs globaux autorises: totalEquipements=").append(kpis.get("totalEquipements"))
                    .append(", pannesEnCours=").append(kpis.get("pannesEnCours"))
                    .append(", stockCritique=").append(kpis.get("stockCritique"))
                    .append(", disponibilite=").append(kpis.get("tauxDisponibilite")).append("%")
                    .append(", coutReparations=").append(kpis.get("coutTotalReparations")).append('\n');
        } else {
            builder.append("Restriction donnees: ne pas repondre avec des KPIs globaux ni les donnees des autres utilisateurs.\n");
        }
        builder.append("Equipements visibles: total=").append(visibleEquipements.size())
                .append(", disponibles=").append(disponibles)
                .append(", affectes=").append(affectes)
                .append(", enPanne=").append(enPanne).append('\n');
        if (employe) {
            builder.append("Materiel affecte a cet employe:\n");
            visibleEquipements.stream().limit(12)
                    .forEach(equipement -> builder.append("- ").append(equipementLine(equipement)).append('\n'));
        }
        builder.append("Pannes ouvertes visibles:\n");
        visiblePannes.forEach(panne -> builder.append("- ").append(panneLine(panne)).append('\n'));
        if (!employe) {
            builder.append("Stock critique visible:\n");
            stockCritique.forEach(piece -> builder.append("- ").append(piece.getReference())
                    .append(" | ").append(piece.getDesignation())
                    .append(" | stock ").append(piece.getQuantiteStock())
                    .append("/").append(piece.getSeuilMinimum())
                    .append(" | ").append(piece.getLocalisation()).append('\n'));
        }
        if (technicien || globalRole) {
            builder.append("Pieces de rechange disponibles pour diagnostic:\n");
            pieces.findAll().stream()
                    .filter(piece -> piece.getUsage() == UsagePiece.RECHANGE)
                    .sorted(Comparator.comparing(Piece::getReference, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .limit(30)
                    .forEach(piece -> builder.append("- ").append(piece.getReference())
                        .append(" | ").append(piece.getDesignation())
                        .append(" | stock ").append(piece.getQuantiteStock())
                        .append(" | seuil ").append(piece.getSeuilMinimum())
                        .append(" | ").append(piece.getLocalisation()).append('\n'));
        }
        builder.append("Regles metier PT17:\n");
        builder.append("- Pack employe: un seul poste principal, puis ecran/accessoires selon departement.\n");
        builder.append("- Pieces de rechange: restent en stock technique, utilisees pour reparations.\n");
        builder.append("- Workflow panne: admin publie, technicien claim, diagnostic/reparation, done/cloture.\n");
        builder.append("- Priorite demo: pannes hautes, stock sous seuil, equipements en panne, prets en retard.\n");
        return builder.toString();
    }

    private String extractResponseText(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        List<String> texts = new ArrayList<>();
        collectText(root, texts);
        return texts.stream()
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("AI response has no text"));
    }

    private String summarizeOpenAiError(String body) {
        if (body == null || body.isBlank()) {
            return "reponse vide";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            String message = error.path("message").asText("");
            String code = error.path("code").asText("");
            if (!message.isBlank()) {
                return code.isBlank() ? message : message + " (" + code + ")";
            }
        } catch (IOException ignored) {
            // Keep raw response below.
        }
        return body.length() > 220 ? body.substring(0, 220) + "..." : body;
    }

    private void collectText(JsonNode node, List<String> texts) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()
                    && (type == null || "output_text".equals(type.asText()) || "text".equals(type.asText()))) {
                texts.add(text.asText());
            }
            node.fields().forEachRemaining(entry -> collectText(entry.getValue(), texts));
        } else if (node.isArray()) {
            node.forEach(child -> collectText(child, texts));
        }
    }

    private ChatReply parseAiReply(String text) throws IOException {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        JsonNode node = objectMapper.readTree(cleaned);
        List<String> bullets = new ArrayList<>();
        JsonNode bulletNode = node.get("bullets");
        if (bulletNode != null && bulletNode.isArray()) {
            bulletNode.forEach(item -> bullets.add(item.asText()));
        }
        return new ChatReply(
                node.path("title").asText("PT17 Copilot"),
                node.path("answer").asText(cleaned),
                bullets,
                node.path("module").asText("HELP")
        );
    }

    private ChatReply dashboardAnswer(Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return new ChatReply(
                    "Mon espace employe",
                    "Je peux surtout t'aider sur ton materiel affecte, tes pannes declarees et les etapes pour demander support.",
                    List.of(
                            "Demande: 'mon materiel'",
                            "Demande: 'mes pannes'",
                            "Demande: 'comment declarer une panne'"
                    ),
                    "EQUIPEMENTS"
            );
        }
        Map<String, Object> kpis = dashboardService.kpis();
        List<String> bullets = hasRole(authentication, "DIRECTEUR")
                ? List.of(
                        "Risque principal: surveiller les pannes ouvertes et le taux de disponibilite.",
                        "Decision: prevoir budget si le cout de maintenance augmente.",
                        "Stock: les ruptures critiques peuvent ralentir les reparations."
                )
                : List.of(
                        "Priorite: traiter les pannes ouvertes avant d'affecter du nouveau materiel.",
                        "Stock: verifier les references sous seuil avant les packs d'integration.",
                        "Demo: ouvre Dashboard pour voir les graphes et les priorites."
                );
        return new ChatReply(
                hasRole(authentication, "DIRECTEUR") ? "Resume executive PT17" : "Vue globale PT17",
                "Le parc contient " + kpis.get("totalEquipements") + " equipement(s), "
                        + kpis.get("pannesEnCours") + " panne(s) non cloturee(s), "
                        + kpis.get("stockCritique") + " reference(s) sous seuil, et un taux de disponibilite de "
                        + kpis.get("tauxDisponibilite") + "%.",
                bullets,
                "DASHBOARD"
        );
    }

    private ChatReply stockAnswer(String query, Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return new ChatReply(
                    "Stock non accessible",
                    "Le stock global est reserve a l'administration et aux techniciens. Je peux par contre t'aider sur ton materiel ou tes pannes.",
                    List.of("Demande: 'mon materiel'", "Demande: 'mes pannes'", "Demande: 'comment declarer une panne'"),
                    "HELP"
            );
        }
        List<Piece> all = pieces.findAll();
        String category = requestedStockCategory(query);
        if (category != null) {
            List<Piece> matches = all.stream()
                    .filter(piece -> matchesPieceCategory(piece, category))
                    .sorted(Comparator.comparing(Piece::getReference, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();
            int totalQuantity = matches.stream().mapToInt(Piece::getQuantiteStock).sum();
            long critical = matches.stream().filter(Piece::stockCritique).count();
            if (matches.isEmpty()) {
                return new ChatReply(
                        "Stock " + category,
                        "Ma l9itch ta reference dyal " + category + " f stock visible.",
                        List.of(
                                "Jreb reference plus precise: cable RJ45, HDMI, USB-C, laptop, ecran...",
                                "Ila khassek had reference, zidha f page Stock."
                        ),
                        "STOCK"
                );
            }
            return new ChatReply(
                    "Stock " + category,
                    "Kaynin " + totalQuantity + " unite(s) mn " + category + " f stock, m9smin 3la "
                            + matches.size() + " reference(s). " + critical + " reference(s) sous seuil.",
                    matches.stream().limit(8).map(this::pieceLine).toList(),
                    "STOCK"
            );
        }
        List<Piece> critiques = all.stream()
                .filter(Piece::stockCritique)
                .sorted(Comparator.comparingInt(Piece::getQuantiteStock))
                .limit(6)
                .toList();
        long materiel = all.stream().filter(piece -> piece.getUsage() == UsagePiece.MATERIEL).count();
        long rechange = all.stream().filter(piece -> piece.getUsage() == UsagePiece.RECHANGE).count();
        List<String> bullets = critiques.isEmpty()
                ? List.of("Aucune reference sous seuil minimum.", "Le stock est stable pour le moment.")
                : critiques.stream()
                        .map(piece -> piece.getReference() + " - " + piece.getDesignation()
                                + " : stock " + piece.getQuantiteStock() + " / seuil " + piece.getSeuilMinimum())
                        .toList();
        return new ChatReply(
                "Analyse stock",
                "J'ai trouve " + all.size() + " reference(s): " + materiel + " materiel(s) et "
                        + rechange + " piece(s) de rechange. " + critiques.size()
                        + " reference(s) sont prioritaires.",
                bullets,
                "STOCK"
        );
    }

    private String requestedStockCategory(String query) {
        if (containsAny(query, "cable", "cabl", "rj45", "hdmi", "usb", "cat6")) {
            return "cable";
        }
        String equipmentCategory = requestedEquipmentCategory(query);
        if (equipmentCategory != null) {
            return equipmentCategory;
        }
        if (containsAny(query, "batterie", "battery", "bat")) {
            return "batterie";
        }
        if (containsAny(query, "chargeur", "charger", "adapter", "adaptateur")) {
            return "chargeur";
        }
        if (containsAny(query, "ram", "memoire", "memory", "ddr")) {
            return "ram";
        }
        if (containsAny(query, "ssd", "disque", "nvme")) {
            return "ssd";
        }
        if (containsAny(query, "toner", "cartouche")) {
            return "toner";
        }
        return null;
    }

    private boolean matchesPieceCategory(Piece piece, String category) {
        String text = normalize(piece.getReference() + " " + piece.getDesignation() + " " + piece.getLocalisation());
        return switch (category) {
            case "cable" -> containsAny(text, "cable", "rj45", "hdmi", "usb-c", "usb", "cat6", "cat-6");
            case "laptop" -> containsAny(text, "pc-", "laptop", "portable", "macbook", "thinkpad", "elitebook", "latitude", "vostro");
            case "desktop" -> containsAny(text, "desk", "desktop", "thinkcentre", "optiplex", "tour", "tiny");
            case "ecran" -> containsAny(text, "ecran", "screen", "monitor", "pouces");
            case "imprimante" -> containsAny(text, "imprimante", "printer", "laserjet", "canon", "epson", "imp-");
            case "clavier" -> containsAny(text, "clavier", "keyboard");
            case "souris" -> containsAny(text, "souris", "mouse");
            case "casque" -> containsAny(text, "casque", "headset", "jabra");
            case "dock" -> containsAny(text, "dock", "station", "wd19");
            case "reseau" -> containsAny(text, "switch", "reseau", "rj45", "cisco", "ubiquiti", "point acces", "ap-");
            case "batterie" -> containsAny(text, "batterie", "battery", "bat-");
            case "chargeur" -> containsAny(text, "chargeur", "charger", "adapter", "adaptateur");
            case "ram" -> containsAny(text, "ram", "memoire", "memory", "ddr");
            case "ssd" -> containsAny(text, "ssd", "nvme", "disque");
            case "toner" -> containsAny(text, "toner", "cartouche");
            default -> false;
        };
    }

    private String pieceLine(Piece piece) {
        String status = piece.stockCritique() ? "sous seuil" : "ok";
        return piece.getReference() + " - " + piece.getDesignation()
                + " - stock " + piece.getQuantiteStock()
                + " / seuil " + piece.getSeuilMinimum()
                + " - " + status;
    }

    private ChatReply actionsAnswer(Authentication authentication) {
        List<Panne> visibles = visiblePannes(authentication);
        long hautes = visibles.stream()
                .filter(panne -> panne.getUrgence() != null && "HAUTE".equals(panne.getUrgence().name()))
                .filter(panne -> panne.getStatut() != StatutPanne.CLOTUREE)
                .count();
        long aAffecter = visibles.stream().filter(panne -> panne.getStatut() == StatutPanne.A_AFFECTER).count();
        long enCours = visibles.stream().filter(panne -> panne.getStatut() == StatutPanne.EN_COURS).count();
        List<String> bullets = visibles.stream()
                .filter(panne -> isActionableForRole(panne, authentication))
                .sorted(Comparator.comparing(Panne::getDateDeclaration, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::panneLine)
                .toList();
        if (bullets.isEmpty()) {
            bullets = List.of("Aucune action urgente detectee.", "Le workflow pannes est propre pour ce profil.");
        }
        return new ChatReply(
                "Actions a faire",
                "Situation: " + hautes + " panne(s) haute urgence, " + aAffecter
                        + " a affecter, " + enCours + " en cours.",
                bullets,
                "PANNES"
        );
    }

    private ChatReply equipementAnswer(String query, Authentication authentication) {
        List<Equipement> visibles = hasRole(authentication, "EMPLOYE")
                ? equipements.findByUtilisateurLogin(authentication.getName())
                : equipements.findAll();
        boolean personalQuestion = containsAny(query, "3andi", "andi", "dyali", "mes", "mon", "ma piece",
                "mes pieces", "mon materiel", "equipement dyali", "materiel dyali");
        if (personalQuestion) {
            String category = requestedEquipmentCategory(query);
            List<Equipement> scoped = category == null
                    ? visibles
                    : visibles.stream()
                            .filter(equipement -> matchesEquipmentCategory(equipement, category))
                            .toList();
            String scopeLabel = category == null ? "equipements" : category;
            String title = category == null
                    ? (hasRole(authentication, "EMPLOYE") ? "Materiel dyalek" : "Equipements visibles")
                    : "Stock " + scopeLabel;
            if (scoped.isEmpty()) {
                return new ChatReply(
                        title,
                        "Ma kayban 3andek ta " + scopeLabel + " f had perimetre.",
                        List.of("Ila khassek had type, t9der tdir demande wla tsol admin.", "Ila kayn erreur, verifier affectation dyal employe."),
                        "EQUIPEMENTS"
                );
            }
            return new ChatReply(
                    title,
                    "3andek " + scoped.size() + " " + scopeLabel + " visible(s).",
                    scoped.stream().limit(10).map(this::equipementLine).toList(),
                    "EQUIPEMENTS"
            );
        }
        if (visibles.isEmpty()) {
            return new ChatReply(
                    "Mon materiel",
                    "Ma kayban 3andek ta equipement affecte daba.",
                    List.of("Ila khassek materiel, t9der tdir demande wla tsol admin.", "Ila kayn erreur f compte, verifier l'affectation employe."),
                    "EQUIPEMENTS"
            );
        }
        List<Equipement> matches = visibles.stream()
                .filter(equipement -> normalize(equipement.getNumSerie() + " " + equipement.getType() + " "
                        + equipement.getMarque() + " " + equipement.getModele()).contains(query)
                        || query.contains(normalize(equipement.getNumSerie())))
                .limit(5)
                .toList();
        if (matches.isEmpty()) {
            long disponible = visibles.stream().filter(e -> e.getStatut() == StatutEquipement.DISPONIBLE).count();
            long enPanne = visibles.stream().filter(e -> e.getStatut() == StatutEquipement.EN_PANNE).count();
            return new ChatReply(
                    "Equipements",
                    "Je n'ai pas trouve de numero de serie precis dans ta question. Dans le perimetre visible: "
                            + visibles.size() + " equipement(s), " + disponible + " disponible(s), "
                            + enPanne + " en panne.",
                    List.of("Essaie: 'cherche PC001' ou 'etat DELL-7430'.", "Tu peux aussi utiliser la recherche globale en haut."),
                    "EQUIPEMENTS"
            );
        }
        return new ChatReply(
                "Resultats equipement",
                "J'ai trouve " + matches.size() + " equipement(s) correspondant(s).",
                matches.stream().map(this::equipementLine).toList(),
                "EQUIPEMENTS"
        );
    }

    private String requestedEquipmentCategory(String query) {
        if (containsAny(query, "laptop", "pc portable", "ordinateur portable")) {
            return "laptop";
        }
        if (containsAny(query, "desktop", "poste", "unite", "thinkcentre")) {
            return "desktop";
        }
        if (containsAny(query, "ecran", "screen", "monitor")) {
            return "ecran";
        }
        if (containsAny(query, "imprimante", "printer", "print")) {
            return "imprimante";
        }
        if (containsAny(query, "clavier", "keyboard")) {
            return "clavier";
        }
        if (containsAny(query, "souris", "mouse")) {
            return "souris";
        }
        if (containsAny(query, "casque", "headset")) {
            return "casque";
        }
        if (containsAny(query, "dock", "station")) {
            return "dock";
        }
        if (containsAny(query, "switch", "reseau", "network", "rj45")) {
            return "reseau";
        }
        return null;
    }

    private boolean matchesEquipmentCategory(Equipement equipement, String category) {
        String text = normalize(equipement.getType() + " " + equipement.getMarque() + " " + equipement.getModele()
                + " " + equipement.getNumSerie());
        return switch (category) {
            case "laptop" -> containsAny(text, "laptop", "portable", "macbook", "thinkpad", "elitebook", "latitude", "vostro");
            case "desktop" -> containsAny(text, "desktop", "desk", "thinkcentre", "optiplex", "tour", "tiny");
            case "ecran" -> containsAny(text, "ecran", "screen", "monitor", "pouces");
            case "imprimante" -> containsAny(text, "imprimante", "printer", "laserjet", "canon", "epson");
            case "clavier" -> containsAny(text, "clavier", "keyboard");
            case "souris" -> containsAny(text, "souris", "mouse");
            case "casque" -> containsAny(text, "casque", "headset", "jabra");
            case "dock" -> containsAny(text, "dock", "station", "wd19");
            case "reseau" -> containsAny(text, "switch", "reseau", "rj45", "cisco", "ubiquiti", "point acces");
            default -> false;
        };
    }

    private ChatReply packAnswer(String query, Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return new ChatReply(
                    "Pack employe",
                    "Je ne peux pas affecter un pack depuis ton role. L'admin choisit le pack d'integration, et toi tu peux suivre le materiel affecte.",
                    List.of("Demande a l'admin si un accessoire manque.", "Declare une panne si un equipement ne fonctionne pas.", "Consulte ton inventaire dans Equipements."),
                    "PACK"
            );
        }
        Map<String, String> presets = new LinkedHashMap<>();
        presets.put("Developpeur", "Laptop performant + ecran 27 pouces + dock USB-C + clavier + souris + casque.");
        presets.put("Finance", "Laptop stable + ecran 24 pouces + clavier/souris + imprimante partagee si besoin.");
        presets.put("Support IT", "Laptop/desktop + casque + accessoires reseau + acces rapide aux pieces de rechange.");
        presets.put("Direction", "Laptop premium + ecran + dock + casque sans fil + webcam.");
        String preset = presets.entrySet().stream()
                .filter(entry -> query.contains(normalize(entry.getKey())))
                .map(entry -> entry.getKey() + " : " + entry.getValue())
                .findFirst()
                .orElse("Je recommande de choisir le departement puis utiliser le bouton AI Pack pour appliquer le preset adapte.");
        return new ChatReply(
                "Assistant pack",
                preset,
                List.of(
                        "Regle pro: un seul poste principal par employe.",
                        "Les pieces de rechange restent dans le stock technique, elles ne doivent pas partir dans le pack employe.",
                        "Avant affectation, verifier le stock critique."
                ),
                "PACK"
        );
    }

    private ChatReply declarePanneHelp() {
        return new ChatReply(
                "Declarer une panne",
                "Bach tdeclari panne, dkhol l page Pannes, khtar equipement dyalek, chrah l mouchkil b ikhtissar, w 7edded urgence ila kan mohim.",
                List.of(
                        "Kteb description wad7a: chno wa9e3, fo9ach bda, wach kayt3awed.",
                        "Ila momkin zid photo bach technicien yfhem b sor3a.",
                        "Men b3d t9der tswlni: 'fin wslt panne dyali ?'"
                ),
                "PANNES"
        );
    }

    private ChatReply smartFallback(String query, Authentication authentication) {
        if (query.matches(".*[a-z]{2,}[-_][a-z0-9].*") || query.matches(".*\\d{2,}.*")) {
            return equipementAnswer(query, authentication);
        }
        return help(authentication);
    }

    private ChatReply help(Authentication authentication) {
        if (hasRole(authentication, "TECHNICIEN")) {
            return new ChatReply(
                    "PT17 Copilot technicien",
                    "Je peux t'aider a prioriser tes pannes et pieces. Ecris en francais, darija ou melange, ana fahmek.",
                    List.of(
                            "Exemple: 'chno ndir lyom ?'",
                            "Exemple: 'pannes li khasni nclaimi'",
                            "Exemple: 'wach kayn chi piece na9sa ?'",
                            "Exemple: 'fin wslat reparation ?'"
                    ),
                    "HELP"
            );
        }
        if (hasRole(authentication, "EMPLOYE")) {
            return new ChatReply(
                    "PT17 Copilot employe",
                    "Je peux t'aider sur ton materiel et tes pannes. Tqder tkteb bdarija, francais, wla melange.",
                    List.of(
                            "Exemple: 'chno 3andi mn materiel ?'",
                            "Exemple: 'bghit ndeclari panne'",
                            "Exemple: 'fin wslt panne dyali ?'",
                            "Exemple: 'pc dyali fih mouchkil'"
                    ),
                    "HELP"
            );
        }
        if (hasRole(authentication, "DIRECTEUR")) {
            return new ChatReply(
                    "PT17 Copilot direction",
                    "Je peux resumer les KPIs, risques et couts. Tu peux poser la question librement.",
                    List.of(
                            "Exemple: 'donne moi resume executive'",
                            "Exemple: 'chno les risques daba ?'",
                            "Exemple: 'cout maintenance ch7al ?'",
                            "Exemple: 'disponibilite wach mzyana ?'"
                    ),
                    "HELP"
            );
        }
        return new ChatReply(
                "PT17 Copilot admin",
                "Je peux t'aider a piloter le parc IT. Tu peux me parler en francais, darija ou avec des mots melanges.",
                List.of(
                        "Exemple: 'chno khasni ndir lyom ?'",
                        "Exemple: 'stock 9lil fin kayn ?'",
                        "Exemple: 'fin kayn PC001 ?'",
                        "Exemple: 'propose pack developpeur'"
                ),
                "HELP"
        );
    }

    private String roleGuide(Authentication authentication) {
        if (hasRole(authentication, "ADMIN")) {
            return """
                    Role ADMIN:
                    - Reponds comme assistant de pilotage IT global.
                    - Priorise: pannes ouvertes, stock critique, affectations, packs employes, notifications.
                    - Tu peux proposer des actions administratives, mais pas executer de suppression.
                    - Donne une synthese claire avec prochaines actions.
                    """;
        }
        if (hasRole(authentication, "TECHNICIEN")) {
            return """
                    Role TECHNICIEN:
                    - Reponds comme assistant terrain.
                    - Montre seulement les pannes visibles pour le technicien: ses pannes + pannes A_AFFECTER.
                    - Aide a diagnostiquer, choisir pieces de rechange, prioriser HAUTE urgence, expliquer next step claim/done.
                    - Evite les sujets admin comme creation comptes, budgets globaux ou gestion utilisateurs.
                    """;
        }
        if (hasRole(authentication, "EMPLOYE")) {
            return """
                    Role EMPLOYE:
                    - Reponds comme support simple.
                    - Aide a comprendre son materiel, ses pannes declarees, comment declarer une panne, quoi faire avant support.
                    - Ne montre pas stock global, autres employes, couts, ni details internes techniciens.
                    - Utilise un ton rassurant et simple.
                    """;
        }
        if (hasRole(authentication, "DIRECTEUR")) {
            return """
                    Role DIRECTEUR:
                    - Reponds comme tableau de bord executive.
                    - Montre KPIs, disponibilite, couts, risques, tendances, decision garder/remplacer.
                    - Evite les details bas niveau sauf si demandes explicitement.
                    - Propose des decisions business: budget, priorites, remplacement.
                    """;
        }
        return "Role inconnu: rester general, prudent, et limiter la reponse aux informations visibles.";
    }

    private List<Panne> visiblePannes(Authentication authentication) {
        if (hasRole(authentication, "EMPLOYE")) {
            return pannes.findByDeclarantLogin(authentication.getName());
        }
        if (hasRole(authentication, "TECHNICIEN")) {
            return pannes.findForTechnicien(authentication.getName(), StatutPanne.A_AFFECTER);
        }
        return pannes.findAll();
    }

    private boolean isActionableForRole(Panne panne, Authentication authentication) {
        if (panne.getStatut() == StatutPanne.CLOTUREE) {
            return false;
        }
        if (hasRole(authentication, "TECHNICIEN")) {
            return panne.getStatut() == StatutPanne.A_AFFECTER
                    || panne.getStatut() == StatutPanne.EN_COURS
                    || panne.getStatut() == StatutPanne.EN_ATTENTE_PIECE;
        }
        if (hasRole(authentication, "EMPLOYE")) {
            return panne.getStatut() != StatutPanne.REPAREE;
        }
        return true;
    }

    private String panneLine(Panne panne) {
        String equipment = panne.getEquipement() == null ? "Sans equipement" : panne.getEquipement().getNumSerie();
        String tech = panne.getTechnicien() == null ? "non affectee" : panne.getTechnicien().getLogin();
        return "#" + panne.getId() + " - " + equipment + " - " + panne.getUrgence()
                + " - " + panne.getStatut() + " - tech: " + tech;
    }

    private String equipementLine(Equipement equipement) {
        String employe = equipement.getEmploye() == null
                ? "non affecte"
                : equipement.getEmploye().getNom() + " " + equipement.getEmploye().getPrenom();
        return equipement.getNumSerie() + " - " + equipement.getType() + " - "
                + safe(equipement.getMarque()) + " " + safe(equipement.getModele())
                + " - " + equipement.getStatut() + " - " + employe;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private boolean containsAny(String query, String... terms) {
        for (String term : terms) {
            if (query.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String text = value == null ? "" : value;
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record ChatReply(String title, String answer, List<String> bullets, String module) {
    }
}
