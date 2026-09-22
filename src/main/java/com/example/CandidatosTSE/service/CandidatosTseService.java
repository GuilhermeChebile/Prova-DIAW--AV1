package com.example.CandidatosTSE.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.example.CandidatosTSE.model.Candidato;
import jakarta.annotation.PostConstruct;

@Service
public class CandidatosTseService {
    private static final String CSV = "data/candidatos/consulta_cand_2026_MG.csv";
    private static final int UF = 10, MUNICIPIO = 12, CARGO = 14, SQ = 15, NUMERO = 16, NOME = 17, URNA = 18,
            CPF = 20, SITUACAO = 23, SIGLA = 26, PARTIDO = 27, NASCIMENTO = 36, GENERO = 39,
            INSTRUCAO = 41, OCUPACAO = 47;
    private List<Candidato> candidatos = new ArrayList<>();

    @PostConstruct
    public void carregarCsv() {
        List<Candidato> lista = new ArrayList<>();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CSV).getInputStream(), StandardCharsets.ISO_8859_1))) {
            leitor.readLine();
            String texto;
            while ((texto = leitor.readLine()) != null) {
                String[] linha = separarCsv(texto);
                if (linha.length <= OCUPACAO) continue;
                Candidato c = new Candidato();
                c.setUf(valor(linha, UF)); c.setMunicipio(valor(linha, MUNICIPIO)); c.setCargo(valor(linha, CARGO));
                c.setSqCandidato(valor(linha, SQ)); c.setNrCandidato(valor(linha, NUMERO));
                c.setNomeCandidato(valor(linha, NOME)); c.setNomeUrna(valor(linha, URNA));
                c.setNrCpfCandidato(valor(linha, CPF)); c.setSituacaoCandidatura(valor(linha, SITUACAO));
                c.setSiglaPartido(valor(linha, SIGLA)); c.setNomePartido(valor(linha, PARTIDO));
                c.setDtNascimento(valor(linha, NASCIMENTO)); c.setGenero(valor(linha, GENERO));
                c.setGrauInstrucao(valor(linha, INSTRUCAO)); c.setOcupacao(valor(linha, OCUPACAO));
                lista.add(c);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler o CSV de candidatos", e);
        }
        resolverFotosPorPessoa(lista);
        lista.forEach(c -> c.setFotoDisponivel(fotoExisteArquivo(c.getNomeArquivoFoto())));
        lista.sort(Comparator.comparing(Candidato::getNomeUrna, Comparator.nullsLast(String::compareTo)));
        candidatos = lista;
    }

    private String[] separarCsv(String linha) {
        List<String> valores = new ArrayList<>(); StringBuilder valor = new StringBuilder(); boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char atual = linha.charAt(i);
            if (atual == '"') {
                if (aspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') { valor.append('"'); i++; }
                else aspas = !aspas;
            } else if (atual == ';' && !aspas) { valores.add(valor.toString()); valor.setLength(0); }
            else valor.append(atual);
        }
        valores.add(valor.toString()); return valores.toArray(String[]::new);
    }

    private void resolverFotosPorPessoa(List<Candidato> lista) {
        var grupos = lista.stream().filter(c -> !c.getNomeCandidato().isBlank() && !c.getNrCandidato().isBlank())
                .collect(Collectors.groupingBy(c -> c.getNomeCandidato().trim().toUpperCase(Locale.forLanguageTag("pt-BR")) + "|" + c.getNrCandidato().trim()));
        for (List<Candidato> grupo : grupos.values()) {
            String foto = grupo.stream().map(Candidato::getSqCandidato).filter(this::fotoExiste).findFirst().orElse(null);
            if (foto == null) continue;
            grupo.stream().filter(c -> !fotoExiste(c.getSqCandidato())).forEach(c -> c.setSqCandidatoParaFoto(foto));
        }
    }

    private boolean fotoExiste(String sq) { return sq != null && new ClassPathResource("static/images/candidatos/FMG" + sq + "_div.jpg").exists(); }
    private boolean fotoExisteArquivo(String nomeArquivo) { return nomeArquivo != null && new ClassPathResource("static/images/candidatos/" + nomeArquivo).exists(); }
    private String valor(String[] linha, int indice) { return linha[indice] == null ? "" : linha[indice].trim(); }
    public List<Candidato> listarTodos() { return candidatos; }
    public List<Candidato> filtrar(String cargo, String partido, String texto) {
        String busca = normalizar(texto);
        return candidatos.stream().filter(c -> vazioOuIgual(cargo, c.getCargo())).filter(c -> vazioOuIgual(partido, c.getSiglaPartido()))
                .filter(c -> busca.isEmpty() || normalizar(c.getNomeCandidato()).contains(busca) || normalizar(c.getNomeUrna()).contains(busca) || normalizar(c.getNrCandidato()).contains(busca)).toList();
    }
    private boolean vazioOuIgual(String filtro, String valor) { return filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase(valor); }
    private String normalizar(String texto) { return texto == null ? "" : texto.trim().toLowerCase(Locale.forLanguageTag("pt-BR")); }
    public List<String> listarCargos() { return distintos(candidatos.stream().map(Candidato::getCargo).toList()); }
    public List<String> listarPartidos() { return distintos(candidatos.stream().map(Candidato::getSiglaPartido).toList()); }
    private List<String> distintos(List<String> valores) { return valores.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).collect(Collectors.toCollection(TreeSet::new)).stream().toList(); }
}
