package fr.lteconsulting.pomexplorer;

import fr.lteconsulting.pomexplorer.graph.PomGraph.PomGraphReadTransaction;
import fr.lteconsulting.pomexplorer.graph.ProjectRepository;
import fr.lteconsulting.pomexplorer.graph.relation.BuildDependencyRelation;
import fr.lteconsulting.pomexplorer.graph.relation.DependencyLikeRelation;
import fr.lteconsulting.pomexplorer.graph.relation.DependencyManagementRelation;
import fr.lteconsulting.pomexplorer.graph.relation.DependencyRelation;
import fr.lteconsulting.pomexplorer.model.Gav;
import fr.lteconsulting.pomexplorer.model.GroupArtifact;
import fr.lteconsulting.pomexplorer.model.transitivity.Repository;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class AnalyzerTest
{

	private static final String PROJECT_A = "fr.lteconsulting:a:1.0-SNAPSHOT";
	private static final String PROJECT_A_3 = "fr.lteconsulting:a:3.0-SNAPSHOT";
	private static final String PROJECT_B = "fr.lteconsulting:b:1.0-SNAPSHOT";
	private static final String PROJECT_B_2 = "fr.lteconsulting:b:2.0-SNAPSHOT";
	private static final String PROJECT_C = "fr.lteconsulting:c:1.0-SNAPSHOT";
	private static final String PROJECT_C_3 = "fr.lteconsulting:c:3.0-SNAPSHOT";
	private static final String PROJECT_D = "fr.lteconsulting:d:1.0-SNAPSHOT";
	private static final String PROJECT_E = "fr.lteconsulting:e:2.0-SNAPSHOT";
	private static final String PROJECT_F = "fr.lteconsulting:f:1.5";

	@Test
	public void test01()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set01");
		//assert
		assertProjects(session, 1);
		assertDependenciesAndBuildDependencies(session, "fr.lteconsulting:pom-explorer:1.1-SNAPSHOT", 14, 2);
		assertNullGavs(session, 1);
	}

	@Test
	public void test02()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set02");
		//assert
		assertProjects(session, 5);
		assertDependencies(session, PROJECT_A, new GavIsSelfManaged( PROJECT_B, true ));
		assertDependenciesManagement( session, PROJECT_A,
				new GavIsSelfManaged( PROJECT_B, true ),
				new GavIsSelfManaged( PROJECT_C, true )
		);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged( PROJECT_C, true ));
		assertDependencies(session, PROJECT_C,
			new GavIsSelfManaged( PROJECT_D, true ),
			new GavIsSelfManaged( PROJECT_E, true )
		);
		assertDependencies(session, PROJECT_D, 0);
		assertDependencies(session, PROJECT_E, 0);
		assertNoNullGavs(session);
	}

	@Test
	public void test03()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set03");
		//assert
		assertProjects(session, 4);
		assertDependencies(session, PROJECT_A,
			new GavIsSelfManaged( PROJECT_B, true ),
			new GavIsSelfManaged( PROJECT_C, true )
		);
		assertDependencies(session, PROJECT_B, 0);
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependencies(session, PROJECT_C, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependencies(session, PROJECT_D, 0);
		assertNoNullGavs(session);
	}

	@Test
	public void test04()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set04");
		//assert
		assertProjects(session, 4);
		assertDependencies(session, PROJECT_A,
				new GavIsSelfManaged( PROJECT_B, true ),
				new GavIsSelfManaged( PROJECT_C, true )
		);
		assertDependencies(session, PROJECT_B, 0);
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependencies(session, PROJECT_C, new GavIsSelfManaged( "fr.lteconsulting:toto:1.4-SNAPSHOT", true ));
		assertDependencies(session, PROJECT_D, 0);
		assertNoNullGavs(session);
	}

	@Test
	public void dependencyWithExclusion()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/dependencyWithExclusion");
		//assert
		assertProjects(session, 4);
		assertDependencies(session, PROJECT_A,
				new GavIsSelfManaged( PROJECT_B, true ),
				new GavIsSelfManaged( PROJECT_C, true )
		);
		assertDependencies(session, PROJECT_B, 0);
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependencies(session, PROJECT_C, new GavIsSelfManaged( "fr.lteconsulting:toto:1.4-SNAPSHOT", true ));
		assertDependencies(session, PROJECT_D, 1);
		assertNoNullGavs(session);

		assertDependencyHasExclusion(session, PROJECT_A, PROJECT_B, PROJECT_D);
		assertThat(session.projects().forGav(Gav.parse(PROJECT_D)).getUnresolvedProperties()).containsExactly( "unknown" );
	}

	@Test
	public void dependencyManagementWithExclusion()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/dependencyManagementWithExclusion");
		//assert
		assertProjects(session, 4);
		assertDependencies(session, PROJECT_A,
				new GavIsSelfManaged( PROJECT_B, true ),
				new GavIsSelfManaged( PROJECT_C, true )
		);
		assertDependencies(session, PROJECT_B, 0);
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependencies(session, PROJECT_C, new GavIsSelfManaged( "fr.lteconsulting:toto:1.4-SNAPSHOT", true ));
		assertDependencies(session, PROJECT_D, 0);
		assertNoNullGavs(session);

		assertDependencyManagementHasExclusion(session, PROJECT_A, PROJECT_B, PROJECT_D);
	}

	@Test
	public void test05()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set05");
		//assert
		assertProjects(session, 2);
		assertDependencies(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependenciesManagement(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertParentDependency(session, PROJECT_A, PROJECT_B);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged( PROJECT_C, true ));

		List<String> shouldBeMissing = new ArrayList<>();
		shouldBeMissing.add(PROJECT_D);
		shouldBeMissing.add(PROJECT_C);
		shouldBeMissing.add(PROJECT_C);

		session.projects().values().forEach(project ->
		{
			System.out.println("PROJECT " + project);
			System.out.println("DEPENDENCIES");
			session.graph().read().dependencies(project.getGav()).forEach(System.out::println);

			 // Checks that transitive dependencies cannot be resolved
			TransitivityResolver resolver = new TransitivityResolver();
			resolver.getTransitiveDependencyTree(session, project, true, true, null, new PomFileLoader()
			{
				@Override
				public File loadPomFileForGav(Gav gav, List<Repository> additionalRepos, Log log)
				{
					assertTrue(shouldBeMissing.contains(gav.toString()));
					shouldBeMissing.remove(gav.toString());

					return null;
				}
			}, System.out::println);
		});

		assertTrue(shouldBeMissing.isEmpty());
	}

	@Test
	public void test06()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set06");
		//assert
		assertProjects(session, 4);
		assertDependencies(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependenciesManagement(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertParentDependency(session, PROJECT_A, PROJECT_B);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged( PROJECT_C, true ));
		assertDependencies(session, PROJECT_C, 0);
		assertDependencies(session, PROJECT_D, 0);

		session.projects().values().forEach(project ->
		{
			System.out.println("PROJECT " + project);
			System.out.println("DEPENDENCIES");
			session.graph().read().dependencies(project.getGav()).forEach(System.out::println);


			// Checks that transitive dependencies can be resolved
			TransitivityResolver resolver = new TransitivityResolver();
			resolver.getTransitiveDependencyTree(session, project, true, true, null, new PomFileLoader()
			{
				@Override
				public File loadPomFileForGav(Gav gav, List<Repository> additionalRepos, Log log)
				{
					fail("missing gav " + gav + " but should not!");

					return null;
				}
			}, System.out::println);
		});
	}

	@Test
	public void test07()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set07");
		//assert
		assertProjects(session, 2);
		assertDependencies(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependenciesManagement(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertParentDependency(session, PROJECT_A, PROJECT_B);
		assertDependencies(session, PROJECT_B, 0);

		Project project = session.projects().forGav(Gav.parse("fr.lteconsulting:a:1.0-SNAPSHOT"));
		assertNotNull(project);

		Map<GroupArtifact, String> pluginManagement = project.getHierarchicalPluginDependencyManagement(null, null, session.projects(), System.out::println);

		assertEquals(2, pluginManagement.size());
		assertEquals("1.0-SNAPSHOT", pluginManagement.get(new GroupArtifact("fr.lteconsulting", "plugin-a")));
		assertEquals("4", pluginManagement.get(new GroupArtifact("fr.lteconsulting", "plugin-b")));

		Set<Gav> plugins = project.getLocalPluginDependencies(null, session.projects(), System.out::println);

		assertEquals(3, plugins.size());
		assertTrue(plugins.contains(new Gav("fr.lteconsulting", "plugin-a", "1.0-SNAPSHOT")));
		assertTrue(plugins.contains(new Gav("fr.lteconsulting", "plugin-b", "4")));
		assertTrue(plugins.contains(new Gav("fr.lteconsulting", "plugin-c", "5")));
	}

	@Test
	public void test08()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/set08");
		//assert
		assertProjects(session, 1);
		assertDependencies(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));
		assertDependenciesManagement(session, PROJECT_A, new GavIsSelfManaged( PROJECT_D, true ));

		Project project = session.projects().forGav(Gav.parse("fr.lteconsulting:a:1.0-SNAPSHOT"));
		assertNotNull(project);
	}


	@Test
	public void multiModule()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/multiModule_inSubfolders");
		//assert
		assertProjects(session, 6);
		assertDependencies(session, PROJECT_A, 0);
		assertDependenciesManagement(session, PROJECT_A,
				new GavIsSelfManaged(PROJECT_B, true),
				new GavIsSelfManaged(PROJECT_B, true),
				new GavIsSelfManaged(PROJECT_E, true),
				new GavIsSelfManaged(PROJECT_E, true),
				new GavIsSelfManaged(PROJECT_F, true)
		);
		assertDependencies(session, PROJECT_B,
				new GavIsSelfManaged(PROJECT_E, true),
				new GavIsSelfManaged(PROJECT_F, false)
		);
		assertParentDependency(session, PROJECT_B, PROJECT_A);
		assertDependencies(session, PROJECT_C,
				new GavIsSelfManaged(PROJECT_B, false),
				new GavIsSelfManaged(PROJECT_B, false),
				new GavIsSelfManaged(PROJECT_E, false),
				new GavIsSelfManaged(PROJECT_F, true)
		);
		assertParentDependency(session, PROJECT_C, PROJECT_A);
		assertDependencies(session, PROJECT_D,
				new GavIsSelfManaged(PROJECT_B, false),
				new GavIsSelfManaged(PROJECT_B, false),
				new GavIsSelfManaged(PROJECT_C, true)
		);
		assertParentDependency(session, PROJECT_D, PROJECT_A);
		assertDependencies(session, PROJECT_E, new GavIsSelfManaged( PROJECT_F, true ));
		assertDependencies(session, PROJECT_F, 0);
		assertNoNullGavs(session);
	}


	@Test
	public void getSubmodules_submodulesInSubFolders()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/multiModule_inSubfolders");
		//assert
		assertProjects(session, 6);
		assertSubmodules(session, PROJECT_A,
				PROJECT_B,
				PROJECT_C,
				PROJECT_D
		);
	}


	@Test
	public void getSubmodules_submodulesInSameFolder()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/multiModule_inSameFolder");
		//assert
		assertProjects(session, 6);
		assertSubmodules(session, PROJECT_A,
				PROJECT_B,
				PROJECT_C,
				PROJECT_D
		);
	}

	@Test
	public void getSubmodules_submoduleIsAlsoMultiModule()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/multiModule_nested");
		//assert
		assertProjects(session, 6);
		assertSubmodules(session, PROJECT_A, PROJECT_B, PROJECT_C );
		assertSubmodules(session, PROJECT_B, PROJECT_D );
	}

	@Test
	public void pomDependency()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/pomDependency");
		//assert
		assertProjects(session, 6);
		assertDependencies(session, PROJECT_A, 0);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged( PROJECT_E, true ));
		assertTransitiveDependency(session, PROJECT_B, new GavIsSelfManaged( PROJECT_E, true ), new GavIsSelfManaged( PROJECT_F, true ));
		assertParentDependency(session, PROJECT_B, PROJECT_A);
		assertDependencies(session, PROJECT_C, 4);
		assertParentDependency(session, PROJECT_C, PROJECT_A);
		assertDependencies(session, PROJECT_D, 3);
		assertParentDependency(session, PROJECT_D, PROJECT_A);
		assertDependencies(session, PROJECT_E, 1);
		assertDependencies(session, PROJECT_F, 0);
		assertNoNullGavs(session);
	}

	@Test
	public void bomDependency()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/bomDependency");
		//assert
		assertProjects(session, 3);
		assertDependencies(session, PROJECT_A, 0);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged(PROJECT_A, false));
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged(PROJECT_C, true));
		assertDependenciesManagement(session, PROJECT_C, new GavIsSelfManaged(PROJECT_A, true));
		assertNoNullGavs(session);
	}

	@Test
	public void bomDependencyProjectVersion()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/bomDependencyProjectVersion");
		//assert
		assertProjects(session, 3);
		assertDependencies(session, PROJECT_A, 0);
		assertDependencies(session, PROJECT_B_2, new GavIsSelfManaged(PROJECT_A_3, false));
		assertDependenciesManagement(session, PROJECT_B_2, new GavIsSelfManaged(PROJECT_C_3, true));
		assertDependenciesManagement(session, PROJECT_C_3, new GavIsSelfManaged(PROJECT_A_3, true));
		assertNoNullGavs(session);
	}

	@Test
	public void bomDependency_dependencyToItself()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/bomDependencyWithSelfDependency");
		//assert
		assertProjects(session, 3);
		assertDependencies(session, PROJECT_A, 0);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged(PROJECT_A, false));
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged(PROJECT_C, true));
		assertDependenciesManagement(session, PROJECT_C, new GavIsSelfManaged(PROJECT_A, true));
		assertNoNullGavs(session);
	}

	@Test
	public void bomDependencyInParent()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/bomDependencyInParent");
		//assert
		assertProjects(session, 4);
		assertDependencies(session, PROJECT_A, 0);
		assertDependencies(session, PROJECT_B, new GavIsSelfManaged(PROJECT_A, false));
		assertDependenciesManagement(session, PROJECT_B, new GavIsSelfManaged(PROJECT_C, true));
		assertDependenciesManagement(session, PROJECT_C, new GavIsSelfManaged(PROJECT_A, true));
		assertDependenciesManagement(session, PROJECT_D, new GavIsSelfManaged(PROJECT_A, false));
		assertNoNullGavs(session);
	}

	@Test
	public void versionInUnresolvedParent()
	{
		//arrange
		Session session = new Session();
		//act
		PomAnalysis.runFullRecursiveAnalysis("testSets/versionInUnresolvedParent", session, new DefaultPomFileLoader( session, false ), null, true, System.out::println);
		//assert
		assertProjects(session, 3);
		assertDependencies(session, PROJECT_A, 0);
		assertDependenciesManagement(session, PROJECT_A,
				new GavIsSelfManaged(PROJECT_B, true),
				new GavIsSelfManaged(PROJECT_B, true)
		);
		assertDependencies(session, PROJECT_B, 0);
		assertParentDependency( session, PROJECT_B, PROJECT_A );
		assertDependencies(session, PROJECT_C,
				new GavIsSelfManaged("fr.lteconsulting:b:null", false),
				new GavIsSelfManaged("fr.lteconsulting:b:null", false)
		);
		assertNullGavs(session, 1);
	}

	@Test
	@Ignore("Regression test for #48")
	public void unresolvedParent()
	{
		//arrange
		Session session = new Session();
		//act
		runFullRecursiveAnalysis(session, "testSets/unresolvedParent");
		//assert
		assertProjects(session, 1);
		assertDependencies(session, PROJECT_A, 1);
		assertParentDependency(session, PROJECT_A, PROJECT_C);

		List<String> shouldBeMissing = new ArrayList<>();
		shouldBeMissing.add(PROJECT_D);
		shouldBeMissing.add(PROJECT_C);

		session.projects().values().forEach(project ->
		{
			System.out.println("PROJECT " + project);
			System.out.println("DEPENDENCIES");
			session.graph().read().dependencies(project.getGav()).forEach(System.out::println);

			// Checks that transitive dependencies cannot be resolved
			TransitivityResolver resolver = new TransitivityResolver();
			resolver.getTransitiveDependencyTree(session, project, true, true, null, new PomFileLoader()
			{
				@Override
				public File loadPomFileForGav(Gav gav, List<Repository> additionalRepos, Log log)
				{
					assertTrue(shouldBeMissing.contains(gav.toString()));
					shouldBeMissing.remove(gav.toString());

					return null;
				}
			}, System.out::println);
		});

		assertTrue(shouldBeMissing.isEmpty());
	}

	@Test
	public void pomWithoutGroupId()
	{
		//arrange
		Session session = new Session();
		//act
		PomAnalysis pomAnalysis = runFullRecursiveAnalysis(session, "testSets/pomWithoutGroupId");
		//assert
		assertProjects(session, 0);
		assertPomFilesWithErrors(pomAnalysis, 1);
	}

	@Test
	public void pomWithoutVersion()
	{
		//arrange
		Session session = new Session();
		//act
		PomAnalysis pomAnalysis = runFullRecursiveAnalysis(session, "testSets/pomWithoutVersion");
		//assert
		assertProjects(session, 0);
		assertPomFilesWithErrors(pomAnalysis, 1);
	}


	@Test
	public void localTest1()
	{
		Session session = new Session();

		try {
			String directory;
			// directory = "c:\\Documents\\Repos\\formation-programmation-java\\projets\\javaee\\cartes-webapp";
			directory = "c:\\Documents\\Repos";
			PomAnalysis.runFullRecursiveAnalysis(directory, session, new DefaultPomFileLoader(session, true), null, true, System.out::println);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("DEPENDENCIES");
		session.graph().read().dependencies(Gav.parse("fr.lteconsulting:pom-explorer:1.1-SNAPSHOT")).forEach(System.out::println);

		System.out.println("BUILD DEPENDENCIES");
		session.graph().read().buildDependencies(Gav.parse("fr.lteconsulting:pom-explorer:1.1-SNAPSHOT")).forEach(System.out::println);

		System.out.println("NULL VERSION GAVS");
		PomGraphReadTransaction tx = session.graph().read();
		tx.gavs().stream()
				.sorted(Comparator.comparing(Gav::toString))
				.filter(gav -> gav.getVersion() == null)
				.forEach(gav ->
				{
					System.out.println(gav);
					tx.dependenciesRec(gav).stream().forEach(r -> System.out.println(" - " + r));
				});
	}

	@Test
	public void localTest2()
	{
		Session session = new Session();

		Log log = System.out::println;

		DefaultPomFileLoader pomLoader = new DefaultPomFileLoader(session, true);

		PomAnalysis analyzis = new PomAnalysis(session, pomLoader, null, false, log);

		analyzis.addDirectory("c:\\Documents\\Repos");
		analyzis.addDirectory("C:\\Users\\Arnaud\\.m2\\repository");

		analyzis.loadProjects();

		analyzis.completeLoadedProjects();

		analyzis.addCompletedProjectsToSession();

		analyzis.addCompletedProjectsToGraph();

		System.out.println("GAVS");
		PomGraphReadTransaction tx = session.graph().read();
		tx.gavs().stream()
				.sorted(Comparator.comparing(Gav::toString))
				.forEach(System.out::println);
	}


	private PomAnalysis runFullRecursiveAnalysis(Session session, String testSet)
	{
		return PomAnalysis.runFullRecursiveAnalysis(testSet, session, null, null, true, System.out::println);
	}

	private void assertProjects(Session session, int numberOfProjects)
	{
		ProjectRepository projects = session.projects();
		projects.values().forEach(project -> System.out.println("PROJECT " + project));
		assertEquals("number of projects", numberOfProjects, projects.size());
	}


	private void assertPomFilesWithErrors(PomAnalysis pomAnalysis, int numberOfPomFiles)
	{
		System.out.println("ERRONEOUS POM FILES");
		List<PomReadingException> files = pomAnalysis.getErroneousPomFiles();
		files.forEach(System.out::println);
		assertEquals("number of projects with errors", numberOfPomFiles, files.size());
	}

	private void assertDependenciesAndBuildDependencies(Session session, String gavString, int numberOfDependencies, int numberOfBuildDependencies)
	{
		assertDependencies(session, gavString, numberOfDependencies, "DEPENDENCIES RESULT FOR " + gavString + "\ndependencies:");
		assertBuildDependencies(session, gavString, numberOfBuildDependencies);
	}

	private void assertDependencies(Session session, String gavString, GavIsSelfManaged... gavs)
	{
		System.out.println("DEPENDENCIES OF " + gavString);
		Set<DependencyRelation> dependencies = session.graph().read().dependencies(Gav.parse(gavString));
		assertDependencies(gavs, dependencies);
	}

	private void assertDependenciesManagement(Session session, String gavString, GavIsSelfManaged... gavs)
	{
		System.out.println("DEPENDENCIES MANAGEMENT OF " + gavString);
		Set<DependencyManagementRelation> dependencies = session.graph().read().dependenciesManagement(Gav.parse(gavString));
		assertDependencies(gavs, dependencies);
	}

	private void assertDependencies(GavIsSelfManaged[] gavs, Set<? extends DependencyLikeRelation> dependencies)
	{
		List<GavIsSelfManaged> actualGavs = dependencies.stream().map( x ->
				new GavIsSelfManaged( x.getTarget().toString(), x.getDependency().isVersionSelfManaged().orElse( null ) )
		).collect( Collectors.toList());
		assertThat(actualGavs).containsExactlyInAnyOrder(gavs);
	}

	private void assertDependencies(Session session, String gavString, int numberOfDependencies)
	{
		assertDependencies(session, gavString, numberOfDependencies, "DEPENDENCIES OF " + gavString);
	}

	private void assertTransitiveDependency(Session session, String gavString, GavIsSelfManaged... gavs)
	{
		System.out.println("TRANSITIVE DEPENDENCIES OF " + gavString);
		Set<DependencyRelation> dependencies = session.graph().read().dependenciesRec(Gav.parse(gavString));
		assertDependencies( gavs, dependencies );
	}

	private void assertTransitiveDependency(Session session, String gavString, int numberOfDependencies)
	{
		System.out.println("TRANSITIVE DEPENDENCIES OF " + gavString);
		Set<DependencyRelation> dependencies = session.graph().read().dependenciesRec(Gav.parse(gavString));
		dependencies.forEach(System.out::println);
		assertEquals("transitive dependencies of " + gavString, numberOfDependencies, dependencies.size());
	}

	private void assertDependencies(Session session, String gavString, int numberOfDependencies, String message)
	{
		System.out.println(message);
		Set<DependencyRelation> dependencies = session.graph().read().dependencies(Gav.parse(gavString));
		dependencies.forEach(System.out::println);
		assertEquals("dependencies of " + gavString, numberOfDependencies, dependencies.size());
	}

	private void assertBuildDependencies(Session session, String gavString, int numberOfDependencies)
	{
		System.out.println("build dependencies:");
		Set<BuildDependencyRelation> buildDependencies = session.graph().read().buildDependencies(Gav.parse(gavString));
		buildDependencies.forEach(System.out::println);
		assertEquals("build dependencies of " + gavString, numberOfDependencies, buildDependencies.size());
	}

	private void assertParentDependency(Session session, String gavString, String parentGav)
	{
		Gav parent = session.graph().read().parent(Gav.parse(gavString));
		assertEquals("parent dependency of " + gavString, parent, Gav.parse(parentGav));
	}

	private void assertNoNullGavs(Session session)
	{
		assertNullGavs(session, 0);
	}

	private void assertNullGavs(Session session, int numberOfNullGavs)
	{
		System.out.println("NULL VERSION GAVS");
		List<Gav> nullGavs = session.graph().read().gavs().stream()
				.filter(gav -> gav.getVersion() == null)
				.sorted(Comparator.comparing(Gav::toString))
				.collect(Collectors.toList());
		nullGavs.forEach(System.out::println);
		assertEquals("number of null gavs", numberOfNullGavs, nullGavs.size());
	}


	private void assertSubmodules( Session session, String multiModule, String... projects )
	{
		Gav multiModuleGav = Gav.parse( multiModule );
		List<Gav> submodules = session.projects().getSubmodules( multiModuleGav);
		List<Gav> expectedModules = Arrays.stream( projects )
				.map( Gav::parse )
				.collect( Collectors.toList());
		assertThat(submodules)
				.as( "submodules of "+multiModule )
				.containsExactlyInAnyOrderElementsOf( expectedModules );
	}

	private void assertDependencyHasExclusion( Session session, String gavString, String dependencyGavString, String excludedProjectGavString )
	{
		assertHasExclusion( gavString, dependencyGavString, excludedProjectGavString, session.graph().read().dependencies(Gav.parse(gavString)));
	}

	private void assertDependencyManagementHasExclusion( Session session, String gavString, String dependencyGavString, String excludedProjectGavString )
	{
		assertHasExclusion( gavString, dependencyGavString, excludedProjectGavString, session.graph().read().dependenciesManagement(Gav.parse(gavString)));
	}

	private void assertHasExclusion(String gavString, String dependencyGavString, String excludedProjectGavString, Set<? extends DependencyLikeRelation> dependencies)
	{
		Gav dependencyGav = Gav.parse( dependencyGavString );
		DependencyLikeRelation relation = dependencies.stream()
				.filter( gav -> gav.getTarget().equals(dependencyGav))
				.findFirst().orElseThrow( () -> new AssertionError( gavString + " does not have a dependency to "+dependencyGavString ) );
		Gav excludedProjectGav = Gav.parse( excludedProjectGavString );
		Set<GroupArtifact> exclusions = relation.getDependency().getExclusions();
		assertThat(exclusions).containsExactly( new GroupArtifact(excludedProjectGav.getGroupId(), excludedProjectGav.getArtifactId()));
	}

	private static class GavIsSelfManaged {
		String gav;
		boolean isSelfManaged;

		GavIsSelfManaged( String gav, boolean isSelfManaged )
		{
			this.gav = gav;
			this.isSelfManaged = isSelfManaged;
		}

		@Override
		public boolean equals( Object o )
		{
			if( this == o ) return true;
			if( o == null || getClass() != o.getClass() ) return false;
			GavIsSelfManaged that = ( GavIsSelfManaged ) o;
			return isSelfManaged == that.isSelfManaged &&
					Objects.equals( gav, that.gav );
		}

		@Override
		public int hashCode()
		{
			return Objects.hash( gav, isSelfManaged );
		}

		@Override
		public String toString()
		{
			return gav + " managed: " + isSelfManaged;
		}
	}
}
