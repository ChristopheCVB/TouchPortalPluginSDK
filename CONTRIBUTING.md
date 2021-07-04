# Contributing

When contributing to this repository, please first discuss the change you wish to make via issue, or any other method with the owners of this repository before making a change. 

Please note we have a [code of conduct](CODE_OF_CONDUCT.md), please follow it in all your interactions with the project.

## Pull Request Process

1. Open an Issue to discuss it
1. Once the matter is settled, create a fork and a branch from `develop` (i.e. `feat/my_feature`)
1. Implement the changes and write the tests
1. Open the Pull Request from your fork to this repo targeting `develop`
1. Wait for review :)

## CI/CD

All CI/CD is governed by push-to-branch naming semantics, and the [build.gradle](build.gradle) file. All are executed by GitHub Actions (see [.github/workflows](.github/workflows)).
- Release/Snapshot is governed by the `IS_RELEASE` env var set to 'YES' or not.
- Pushes to `develop` trigger snapshot releases to maven central. Creds are in GH secrets.
- Pushes to `release/*` trigger releases to maven central. Creds are in GH secrets.
- Pushes to `master` triggers a build, test and coverage report.

### Publishing Snapshots
Snapshots are available to developers wanting to use the latest version of this library.
While not perfect, any pushes to develop should (1) Compile, (2) pass unit tests, and (3) have been checked manually.

Provided these are satisfied, this is how you can publish a snapshot:
1. Create a branch from `develop` and work on it.
1. Create a PR for merge into the `develop` branch.
1. Merge into `develop`; this triggers a SNAPSHOT release

### Publishing Releases
Releases are available to the general-public as official releases on Maven Central. In addition
to meeting code standards, they also include release notes and a tag in git.

To make an official release, follow this process:
1. When you want to release what is on the `develop` branch, create a new branch with the name `release/MAJOR.MINOR.PATCH`.
1. Push the new release branch; this will create a new release in Maven Central.
1. Create a PR to merge that branch into `master`. Include in that PR any release notes for that release.
1. Merge the PR into master.
1. Checkout the `develop` branch and increment the patch version; this way, future pushes to develop will be against the snapshot of that version. 