# Contributing to this Repository

We would love for you to contribute to this repository and help make it even better than it is
today! As a contributor, here are the guidelines we would like you to follow:

 - [Code of Conduct](#coc)
 - [Question or Problem?](#question)
 - [Issues and Bugs](#issue)
 - [Submission Guidelines](#submit)
 - [Coding Rules](#rules)
 - [Commit Message Guidelines](#commit)

## <a name="coc"></a> Code of Conduct
Help us keep this project open and inclusive. Please read and follow our [Code of Conduct][coc].

## <a name="question"></a> Got a Question or Problem?

Do not open issues for general support questions as we want to keep GitLab issues for bug reports and feature requests. You've got much better chances of getting your question answered on other places, like the Fediverse at [@kura@fedi.z0ne.moe](https://fedi.z0ne.moe/kura).

To save your and our time, we will systematically close all issues that are requests for general support and redirect people to other places.

If you would like to chat about the question in real-time, you can reach out via [our Discord channel][discord].

## <a name="issue"></a> Found a Bug?
If you find a bug, you can help us by
[submitting an issue](#submit-issue) to our [Repository][repo]. Even better, you can
[submit a Pull Request](#submit-pr) with a fix.

## <a name="submit"></a> Submission Guidelines

### <a name="submit-issue"></a> Submitting an Issue

Before you submit an issue, please search the issue tracker, maybe an issue for your problem already exists and the discussion might inform you of workarounds readily available.

We want to fix all the issues as soon as possible, but before fixing a bug we need to reproduce and confirm it. In order to reproduce bugs, we will systematically ask you to provide a way of reproduction. Having a reproducible scenario gives us a wealth of important information without going back & forth to you with additional questions.

Unfortunately, we are not able to investigate / fix bugs without a  reproduction scenario, so if we don't hear back from you, we are going to close an issue that doesn't have enough info to be reproduced.

You can file new issues by selecting from our [new issue templates][new_issue] and filling out the issue template.


### <a name="submit-pr"></a> Submitting a Pull Request (PR)
Before you submit your Pull Request (PR) consider the following guidelines:

1. Search [GitHub][pull_requests] for an open or closed PR
  that relates to your submission. You don't want to duplicate effort.
1. Be sure that an issue describes the problem you're fixing, or documents the design for the feature you'd like to add.
  Discussing the design up front helps to ensure that we're ready to accept your work.
1. Fork the repo.
1. Make your changes in a new git branch:

     ```shell
     git checkout -b my-fix-branch master
     ```

1. Create your patch, **including appropriate test cases**.
1. Follow our [Coding Rules](#rules).
1. Run the full test suite and ensure that all tests pass.
1. Commit your changes using a descriptive commit message that follows our
  [commit message conventions](#commit). Adherence to these conventions
  is necessary because release notes are automatically generated from these messages.

     ```shell
     git commit -a
     ```
    Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

1. Push your branch to GitLab:

    ```shell
    git push origin my-fix-branch
    ```

1. In GitHub, send a pull request to `launcher:master`.
* If we suggest changes then:
  * Make the required updates.
  * Re-run the Angular test suites to ensure tests are still passing.
  * Rebase your branch and force push to your GitLab repository (this will update your Pull Request):

    ```shell
    git rebase master -i
    git push -f
    ```

That's it! Thank you for your contribution!

#### After your pull request is merged

After your pull request is merged, you can safely delete your branch and pull the changes
from the main (upstream) repository:

* Delete the remote branch on GitLab either through the GitLab web UI or your local shell as follows:

    ```shell
    git push origin --delete my-fix-branch
    ```

* Check out the master branch:

    ```shell
    git checkout master -f
    ```

* Delete the local branch:

    ```shell
    git branch -D my-fix-branch
    ```

* Update your master with the latest upstream version:

    ```shell
    git pull --ff upstream master
    ```

## <a name="rules"></a> Coding Rules
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes **must be tested** by one or more specs (unit-tests).
* We use Rusts `rustfmt` tool for the source code.

## <a name="commit"></a> Commit Message Guidelines

We have very precise rules over how our git commit messages can be formatted.  This leads to **more
readable messages** that are easy to follow when looking through the **project history**.  But also,
we use the git commit messages to **generate the change log** using [git-chglog](https://github.com/mattn/git-chglog).

### Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **header** is mandatory and the **scope** of the header is optional.

The first line of the commit must be 50 characters in length, any following line cannot be longer than 100 characters! This allows the message to be easierto read on GitLab as well as in various git tools.

The footer should contain a [closing reference to an issue](https://docs.gitlab.com/ee/user/project/issues/managing_issues.html#closing-issues-automatically) if any.

### Revert
If the commit reverts a previous commit, it should begin with `revert: `, followed by the header of the reverted commit. In the body it should say: `This reverts commit <hash>.`, where the hash is the SHA of the commit being reverted.

### Type
Must be one of the following:

* **Add**: Adds new content and systems.
* **Change**: Changes behavior of existing content and systems
* **Deprecate**: Deprecates soon to be removed content
* **Remove**: Removes content completly
* **Fix**: A bug fix
* **Security**: A code change that improves security or severe issues
* **Art**: New art
* **Performance**: Improves performance or resource usage
* **Test**: Adding missing tests or correcting existing tests
* **Update**: Updates to dependencies and their code adjustments
* **Misc**: Miscellaneous maintenance tasks

### Scope
The scope should be the name of the area affected (as perceived by the person reading the changelog generated from commit messages).

The following is the list of supported scopes:

* **assets**
* **gui**
* **yggdrasil**
* **forge**
* **minecraft**

### Subject
The subject contains a succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize the first letter
* no dot (.) at the end

### Body
Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes".
The body should include the motivation for the change and contrast this with previous behavior.

### Footer
The footer should contain any issues that this commit **Closes**.


[coc]: https://github.com/mcz0ne/launcher/blob/master/CODE_OF_CONDUCT.md
[repo]: https://github.com/mcz0ne/launcher
[new_issue]: https://github.com/mcz0ne/launcher/issues/new
[pull_requests]: https://github.com/mcz0ne/launcher/pulls
